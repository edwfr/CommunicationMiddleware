# CommunicationMiddleware

The widespread use of mobile and wearable devices has led to a growing need of integration between these new technologies. Precisely for this reason the objective of the thesis project was to design and implement a middleware for the devices of these categories that helps communication and coordination among them through high-level patterns, such as message exchange, publish/subscribe, tuple space. In particular, it was decided to base it on Bluetooth technology and to develop it for Android systems, thus managing to cover most of the devices of the aforementioned categories.

## Project Structure

The project is composed by two parts, the one concerning the middleware that is contained in the ```bt_lib```  package and the one concerning a testing application that is contained in the package ```app``` where you can find an example of an application.

## Features
![Platform](http://img.shields.io/badge/platform-android-green.svg?style=flat)
![License](https://img.shields.io/aur/license/yaourt.svg)
![API](https://img.shields.io/badge/API-19%2B-brightgreen.svg?style=flat)

The API provides the following operation:
<br/>
- [x] create client
- [x] create server
- [x] connect
- [x] disconnect
- [x] automatic reconnection following unplanned disconnections
- [x] scan nearby devices
- [x] send message
- [x] receive message
- [x] publish message
- [x] subscribe channel
- [x] out message
- [x] in request message
- [x] read request message
