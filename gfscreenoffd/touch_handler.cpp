/* SPDX-License-Identifier: BSD-3-Clause */

#include "touch_handler.h"

#include <android-base/logging.h>
#include <fcntl.h>
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
    const char eventPathStart[] = "/dev/input/event";
    int fd;
    unsigned long evbit;

    for (int i = 0; i < 10; i++) {
        std::string f(eventPathStart + std::to_string(i));
        if (f.compare(0, sizeof(eventPathStart) - 1, eventPathStart) == 0 &&
            access(f.c_str(), F_OK) != -1) {
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

void Listen(const std::string& eventPath, const ListenerCallback& callback) {
    int fodstat, fodstatfd, evfd;
    pollfd pfd;
    input_event ev;

    evfd = open(eventPath.c_str(), O_RDONLY | O_NONBLOCK);
    fodstatfd = open(fodStatusPath, O_WRONLY | O_NONBLOCK);

    pfd.fd = evfd;
    pfd.events = POLLIN;
    pfd.revents = 0;

    while (true) {
        // unblock touch listener by enabling fod_status
        fodstat = 1;
        write(fodstatfd, &fodstat, 1);

        poll(&pfd, 1, -1);
        if (!(pfd.revents & POLLIN)) {
            continue;
        }

        if (read(pfd.fd, &ev, sizeof(ev)) != sizeof(ev)) {
            LOG(ERROR) << "Invalid event size, exiting.";
            goto out;
        }

        callback(ev);
    }

out:
    close(fodstatfd);
    close(evfd);
}

} // namespace gfscreenoffd
} // namespace chaldeastudio
} // namespace vendor
