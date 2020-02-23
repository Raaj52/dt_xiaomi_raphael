/* SPDX-License-Identifier: BSD-3-Clause */

#define LOG_TAG "gfscreenoffd"

#include <android-base/logging.h>
#include <fcntl.h>
#include <linux/input.h>
#include <linux/uinput.h>
#include <poll.h>
#include <unistd.h>

#include <chrono>
#include <fstream>

#include "touch_handler.h"

using namespace vendor::chaldeastudio::gfscreenoffd;

int main() {
    auto lastdown = std::chrono::high_resolution_clock::now();
    int ret, ufd;
    std::string touchEventPath;
    struct uinput_user_dev udev;

    ufd = open("/dev/uinput", O_WRONLY | O_NONBLOCK);
    if (ufd < 0) {
        LOG(ERROR) << "Unable to open uinput, exiting.";
        return 1;
    }

    // creating virtual input for the front-ends

    ioctl(ufd, UI_SET_EVBIT, EV_KEY);
    ioctl(ufd, UI_SET_KEYBIT, KEY_FOD_GESTURE_DOWN);
    sprintf(udev.name, "uinput-gfscreenoffd");
    udev.id.bustype = BUS_VIRTUAL;

    ret = write(ufd, &udev, sizeof(udev));
    if (ret < 0) {
        LOG(ERROR) << "Failed to create virtual input, exiting.";
        close(ufd);
        return 1;
    }

    ret = ioctl(ufd, UI_DEV_CREATE);
    if (ret < 0) {
        LOG(ERROR) << "Failed to create virtual input, exiting.";
        close(ufd);
        return 1;
    }

    touchEventPath = FindTouchEv();
    if (touchEventPath.empty()) {
        LOG(ERROR) << "No touchscreen detected, exiting.";
        close(ufd);
        return 1;
    }

    // listening to finger area touch and then sending virtual key
    Listen(touchEventPath.c_str(), [&lastdown, &ufd](const input_event& ev) {
        auto now = std::chrono::high_resolution_clock::now();
        double delta = std::chrono::duration<double, std::milli>(now - lastdown).count();

        if (delta > 1000.0 && ev.code == KEY_FOD_SCRNOFF_DOWN && ev.value == 1) {
            Send(ufd, EV_KEY, KEY_FOD_GESTURE_DOWN, 1);
            Send(ufd, EV_KEY, KEY_FOD_GESTURE_DOWN, 0);
            Send(ufd, EV_SYN, SYN_REPORT, 0);
            lastdown = std::chrono::high_resolution_clock::now();
            SendResetState(400);
        }

        return;
    });

    close(ufd);
    return 1;
}
