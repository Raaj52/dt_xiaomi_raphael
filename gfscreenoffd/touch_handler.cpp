/* SPDX-License-Identifier: BSD-3-Clause */
#define LOG_TAG "gfscreenoffd"

#include "touch_handler.h"

#include <android-base/logging.h>
#include <dirent.h>
#include <fcntl.h>
#include <linux/fb.h>
#include <linux/input.h>
#include <poll.h>
#include <unistd.h>

namespace vendor {
namespace chaldeastudio {
namespace gfscreenoffd {

void Send(int fd, int type, int code, int value) {
    struct input_event ev {};

    memset(&ev, 0, sizeof(ev));
    ev.type = type;
    ev.code = code;
    ev.value = value;

    if (write(fd, &ev, sizeof(ev)) < 0) {
        LOG(ERROR) << "Failed to write input " << ev.code;
    }
}

std::string FindTouchEv() {
    DIR* evd;
    int fd, eventTotal = 0;
    unsigned long evbit;

    evd = opendir("/dev/input");
    while (readdir(evd)) eventTotal++;
    closedir(evd);

    for (int i = 0; i <= eventTotal; i++) {
        std::string f("/dev/input/event" + std::to_string(i));
        if (access(f.c_str(), F_OK) != -1) {
            fd = open(f.c_str(), O_RDONLY | O_NONBLOCK);
            ioctl(fd, EVIOCGBIT(0, sizeof(evbit)), &evbit);
            if (evbit & (1 << EV_KEY) && evbit & (1 << EV_ABS)) {
                LOG(INFO) << "Found the touchscreen device at : " << f.c_str();
                close(fd);
                return f;
            }
            close(fd);
        }
    }

    return {};
}

void SendResetState(const int duration) {
    int fodTest = 0;
    int fd = open(fodTestPath, O_WRONLY);

    usleep(1000 * duration);
    write(fd, &fodTest, sizeof(fodTest));

    close(fd);
}

int UnblockFodStatus() {
    int fd, ret, status;

    fd = open(fodStatusPath, O_RDWR | O_NONBLOCK);
    ret = read(fd, &status, sizeof(status));

    if (ret < 0) {
        goto out;
    }

    if (!status) {
        LOG(INFO) << "FOD Touch listener is disabled, enabling.";
        status = 1;
        write(fd, &status, 1);
    }

out:
    close(fd);
    return ret;
}

void Listen(const std::string& eventPath, const ListenerCallback& callback) {
    int fdTouch, fdBlank, ret;
    pollfd pfdTouch, pfdBlank;
    input_event ev;

    fdTouch = open(eventPath.c_str(), O_RDONLY | O_NONBLOCK);
    pfdTouch.fd = fdTouch;
    pfdTouch.events = POLLIN;
    pfdTouch.revents = 0;

    fdBlank = open(fblankPath, O_RDONLY | O_NONBLOCK);
    pfdBlank.fd = fdBlank;
    pfdBlank.events = POLLIN;
    pfdBlank.revents = 0;

    while (true) {
        usleep(10000);

        // wait for screen off
        poll(&pfdBlank, 1, -1);
        if (pfdBlank.revents & POLLIN) {
            if (read(pfdBlank.fd, &ret, sizeof(ret)) < 0) {
                LOG(ERROR) << "Unable to read blank state, exiting.";
                goto out;
            }
            if (ret <= FB_BLANK_NORMAL) {
                usleep(100000);
                continue;
            }
        }

        // unblock touch listener by enabling fod_status
        ret = UnblockFodStatus();
        if (ret < 0) {
            LOG(ERROR) << "Unable to read fod_status, exiting.";
            goto out;
        }

        poll(&pfdTouch, 1, -1);
        if (pfdTouch.revents & POLLIN) {
            if (read(pfdTouch.fd, &ev, sizeof(ev)) != sizeof(ev)) {
                LOG(ERROR) << "Invalid event size, exiting.";
                goto out;
            }
            callback(ev);
        }
    }

out:
    close(fdBlank);
    close(fdTouch);
}

} // namespace gfscreenoffd
} // namespace chaldeastudio
} // namespace vendor
