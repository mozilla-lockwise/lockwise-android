#!/bin/sh

# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/. */

curl -sSLO https://github.com/shyiko/ktlint/releases/download/0.29.0/ktlint &&
  chmod a+x ktlint &&
  sudo mv ktlint /usr/local/bin/

