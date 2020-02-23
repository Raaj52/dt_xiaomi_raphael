/* SPDX-License-Identifier: BSD-3-Clause */

#ifndef _OFF_TOUCH_HANDLER_H
#define _OFF_TOUCH_HANDLER_H

#include <linux/input.h>

#include <functional>

#define KEY_FOD_SCRNOFF_DOWN 325
#define KEY_FOD_GESTURE_DOWN 745

static constexpr const char* fodStatusPath = "/sys/devices/virtual/touch/tp_dev/fod_status";
static constexpr const char* fodTestPath = "/sys/devices/virtual/touch/tp_dev/fod_test";

namespace vendor {
namespace chaldeastudio {
namespace gfscreenoffd {

using ListenerCallback = std::function<void(const input_event&)>;

std::string FindTouchEv();
void Listen(const std::string& eventPath, const ListenerCallback& callback);
void Send(int fd, int type, int code, int value);
void SendResetState(const int duration);

} // namespace gfscreenoffd
} // namespace chaldeastudio
} // namespace vendor
#endif
