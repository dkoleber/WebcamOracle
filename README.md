# Webcam Oracle

This project allows a user to use a webcam and RaspberryPi along with an Android app to setup a security camera system. Additionally, a PC client allows users to retrieve single or multiple frames from the webcam.

The project contains 

* A django server that is run on a Raspi
* An android app which connects to the server on the Raspi
* A django server which can be run on a client machine to query the Raspi server. This has been only tested on Windows.


## Raspi Requirements

The most significant requirement is that OpenCV for python must be built on the Raspi. This can take a few hours.

Other pip packages must be installed including: pycryptodome, django, venv, zipfile


## Windows Requirements

Pip requirements: pycryptodome, venv, zipfile, opencv-python


### I'll probably add more documentation later if I decide to develop the project more

## License

Copyright (c) 2019 Derek Koleber

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.