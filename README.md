Android Things Robocar
============

Introduction
------------
This project contains all the code required to build a robot car that runs on
[Android Things](https://developer.android.com/things/index.html), as well as a companion "controller" Android app.

This sample uses the following Google platforms and APIs:

- [Android Things](https://developer.android.com/things/index.html) - The car's onboard operating system.
- [Nearby APIs](https://developers.google.com/nearby/) - Local communication API used
for pairing the robocar to a companion app which controls the car.


Pre-requisites
--------------
To build the car, you will need the following hardware:

- An [Android-Things powered device](https://developer.android.com/things/hardware/developer-kits.html) with a connected camera.  This demo ran on a Raspberry Pi 3, but is in no way limited to that device.

- A robot car chassis.  We had great luck with the [Runt rover robotics kit](https://www.amazon.com/Actobotics-Junior-Runt-Rover/dp/B00UAWVC64).

- A power source.  The developer board and DC motors each need their own power input, so you can either get two small power packs or something like the [Anker Powercore 13000](https://smile.amazon.com/Anker-PowerCore-13000-Portable-Charger/dp/B00Z9QVE4Q/), which has two USB-out ports.

- The [Adafruit stepper & DC Motor hat](https://www.adafruit.com/product/2348), for controlling the motors.

(Optional)
To assist in distinguishing between multiple robocars during the pairing process, the robocar also supports alphanumeric displays or RGB LED's for displaying a pairing code.  Note that this is meant to _assist_ the user,
and is not intended or sufficient as a security measure.  If you'd like to add these to your setup, you'll need:

- [A quad alphanumeric display](https://www.adafruit.com/product/1912)

- [An RGB Led](https://www.adafruit.com/product/159)

- [A half-size breadboard](https://www.adafruit.com/product/64)


Support and Discussion
-------

- Google+ IoT Developer Community Community: https://plus.google.com/communities/107507328426910012281
- Stack Overflow: https://stackoverflow.com/tags/android-things/

If you've found an error in this sample, please file an issue:
https://github.com/androidthings/robocar/issues

Patches are encouraged, and may be submitted by forking this project and
submitting a pull request through GitHub.

License
-------

Copyright 2017 Google, Inc.

Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements.  See the NOTICE file distributed with this work for
additional information regarding copyright ownership.  The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License.  You may obtain a copy of
the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
License for the specific language governing permissions and limitations under
the License.
