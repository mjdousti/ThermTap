# ThermTap: An Online Power Analyzer and Thermal Simulator for Android Devices

## Change Log
```
Version   |  Change
-------------------------------------------------------------
1.00      |  Initial release.
```

## License
Please refer to the [LICENSE](LICENSE) file.


## Description
ThermTap enables system and software developers to monitor the power consumption and temperature of various hardware components in an Android device as a function of running applications and processes. It comprises of a power analyzer, called PowerTap, and an online thermal simulator, called Therminator 2. With accurate power macro-models, PowerTap collates activity profiles of major components of a portable device from the OS kernel device drivers in an event-driven manner to generate power traces. In turn, Therminator 2 reads these traces and, using a compact thermal model of the device, generates various temperature maps including those for the device components and device skin. Fast thermal simulation techniques enable Therminator 2 to be executed in realtime. With precise per-process and per-application temperature maps that ThermTap produces, it enables software and system developers to find thermal bugs in their software. 



## Directories & Files Structure
```
ThermTap
    |-- PowerTap
        |-- adb_tools -> Android Debug Bridge (adb) binary for Linux
        |-- android
            |-- boot.img -> Modified kernel 3.4 boot image for Nexus 5
            |-- jcat -> A modification of the UNIX 'cat' command which works on circular buffers (e.g., FTace buffer)
            |-- jcat.c -> jcat source code
            |-- kill_stap.sh -> A bash script for killing the SystemTap module
            |-- powertap.ko -> Binary code of the SystemTap kernel module compiled for Nexus 5 (using the given boot.img)
            |-- powertap_lnx3.2.stp -> Source code of the SystemTap kernel module suitable for kernels of version 3.2 or lower
            |-- powertap_lnx3.3.stp -> Source code of the SystemTap kernel module suitable for kernels of version 3.3 or higher
            |-- stapio -> A binary file required for implanting and executing the SystemTap kernel module
            |-- staprun -> A binary file required for implanting and executing the SystemTap kernel module
            `-- start_stap.sh -> A bash script for starting the SystemTap module
        |-- icons
            |-- icons -> icons used in the ThermTap GUI
        |-- libs
            |-- commons-collections4-4.0.jar -> Appache Commons Collections library
            |-- dom4j-1.6.1.jar -> A flexible XML framework for Java
            |-- graphics
                |-- jfreechart-1.0.19 -> A Java chart library
                `-- org.jzy3d-0.9.jar -> Scientific 3D plotting library
        |-- src
            `-- edu -> Java source code directory
        `-- build.xml -> Ant build file
    |-- Therminator-v2
        |--src
            |-- header
                `-- *.h -> header files
            `-- *.cpp -> Therminator source code
        |-- libs
            |-- inotify-cxx -> inotify C++ interface
            `-- pugixml -> Light-weight, simple and fast XML parser for C++
        `-- Makefile -> Therminator makefile
    |-- LICENSE -> license file
    |-- Makefile -> ThermTap makefile
    |-- package_N5.xml -> Nexus 5 design specification file
    `-- README -> this file
```

## Requirements
**DISCLAIMER:** Rooting and unlocking the bootloader of a phone may void its warranty. Make sure you completely understand the steps described below before you proceed. We take absolutely no responsibility on any damage you may cause to your phone by following the provided instructions.

ThermTap requires a Linux machine to run. We have tested ThermTap and its components on Debian 8. The following instructions are prepared for this distribution and requires some changes for other Linux distros.


### Host side (which runs ThermTap):
1.  Ant 1.7 or higher
2.  Oracle Java 7-JDK or higher
3.  GCC 4.8+
4.  Android USB driver
5.  SystemTap 2.3+
6.  Git
7.  Java bindings for OpenGL API plus (JNI lib)
8.  Eigen 3
9.  Boost ODEINT library
10. Intel Math Kernel Library (MKL)


In order to install items 1 through 9, you may run the following command:
```
sudo apt-get install libjogl2-jni libjogl2-java build-essential ant openjdk-7-jdk systemtap android-tools-adb android-tools-fastboot libeigen3-dev libboost1.55-dev git
```

Finally, Intel MKL can be downloaded from [here](https://software.intel.com/en-us/mkl) for free.

Assuming that the installation path is `/opt/intel`, add the following lines to the end of `/etc/profile` and `~/.bashrc`:
```
#Intel Compiler
source /opt/intel/bin/compilervars.sh intel64

#Intel MKL
source /opt/intel/mkl/bin/mklvars.sh intel64
```

### Target side (Android device):
1. Make sure that your device is rooted and its bootloader is unlocked. The rooting and unlocking processes are different for various devices. You may check [XDA-Developers Android Forums](http://forum.xda-developers.com) for the specific instructions for your phone/tablet. The process for Nexus 5 is explained [here](http://forum.xda-developers.com/google-nexus-5/general/guide-nexus-5-how-to-unlock-bootloader-t2507905).

2. Install [SuperSU](https://play.google.com/store/apps/details?id=eu.chainfire.supersu) and [Busybox](https://play.google.com/store/apps/details?id=stericson.busybox) from the Google PlayStore on the device.

3. We provide `boot.img` for Nexus 5 (Android 5.1.1) so you don't need to go through this step for this device (it's located in `ThermTap/PowerTap/android`). Please do NOT use this image for other devices as it may make your phone unbootable. [This link](http://marcin.jabrzyk.eu/posts/2014/05/building-and-booting-nexus-5-kernel) explains how to make an unmodified `boot.img` for Nexus 5.
    

In order to enable the SystemTap module insertion, obtain the source code of the kernel for the device that you have. Make sure the following options are enabled in the kernel configuration:
```
CONFIG_DEBUG_INFO
CONFIG_MODULES
CONFIG_MODULE_UNLOAD
CONFIG_KPROBES
CONFIG_RELAY
CONFIG_DEBUG_FS
CONFIG_FTRACE
```

Also ensure that the following option is `DISABLED`:
```
CONFIG_STRICT_MEMORY_RWX
```

Next, compile the kernel, make `boot.img` and transfer it to your device. To transfer `boot.img` to your device follow these instructions:

* Connect the phone through a USB cable to the PC.
* Open a terminal and navigate to where boot.img is located.
* Issue the following command to put the phone in the fastboot mode:

      $ adb reboot bootloader
* Issue this command to "temporarily" transfer `boot.img` to your device. Note that after one restart, the original `boot.img` of your device replaces the new one. So this command allows you to "only" test your image without bricking the phone.

      $ fastboot boot boot.img
    * When you are sure that the device is working properly with your boot.img, you may permanently transfer it to the device:

          $ adb reboot bootloader
          $ fastboot flash boot boot.img

4. If you used your own `boot.img`, you need to recompile the SystemTap module. Navigate to the `ThermTap/PowerTap/android` directory and issue the following commands (without `$`) inside the terminal. Note that if your kernel version is 3.2 or below, you should replace `powertap.stp` with `powertap_lnx3.2.stp` and with `powertap_lnx3.3.stp`, otherwise. Also, make sure to replace `PLACE_YOUR_COMPILED_KERNEL_DIRECTORY_PATH` with the absolute path your compile kernel directory.

       $ git clone https://android.googlesource.com/platform/prebuilts/gcc/linux-x86/arm/arm-eabi-4.7/
       $ export CROSS_COMPILE=`pwd`/arm-eabi-4.7/bin/arm-eabi-
       $ export LINUX_KERNEL=PLACE_YOUR_COMPILED_KERNEL_DIRECTORY_PATH
       $ export TAPSET=/usr/share/systemtap/tapset
       $ export SYSTEMTAP_RUNTIME=/usr/share/systemtap/runtime
       $ stap -p 4 -v -a arm -r $LINUX_KERNEL -B CROSS_COMPILE=$CROSS_COMPILE -I $TAPSET -R $SYSTEMTAP_RUNTIME -t -g -m powertap powertap.stp

5. Enable the USB debugging on the phone as described next.
   * Open `Settings->About` phone (or About device)
   * Tap the `Build number` five times to enable the `Developer options`.
   * Go back to the `Settings` menu and now you'll be able to see `Developer options` there.
   * Tap it and turn on the `USB Debugging` option from the menu on the next screen.
   * Connect the device to the PC using a USB cable.
   * Open a terminal and issue the following command.
         
         $ adb shell
   * A message will pop up on the phone asking for the permission to allow the PC to access the device.
   * Mark the always allow option and select OK.


## Compilation
Run the following command at the root directory of ThermTap to compile it:

    $ make

This command will clean the built files:

    $ make clean

## Execution

Run the following command at the root directory of ThermTap:

    $ make run

**Note:** ThermTap may not work properly if it is placed in a path which contains space.

## Usage
1. Connect your device to the PC through the USB cable and run ThermTap.
2. Open the design specification file (e.g., `package_N5.xml` for Nexus 5) for the device by clicking on the 'Open' button. You may edit the ambient temperature in this file.
2. Click on the `Connect` button.
3. Click on the `Start` button which initiates reading of information from the device.

### Scenarios
#### Probing the power profile of the entire system:
  * After pressing the `Start` button, ThermTap charts will show the power profile of the whole system. Press click on the `Refresh` button to see the applications running on the system. By choosing a UID or a PID, charts will show the power profile corresponding to the selected UID and/or PID. (`UID = -1` represents the whole system.)
#### Observing the thermal maps of the device components:
  * The thermal analysis can be done in either static or transient form. By clicking on the `Steady` button, ThermTap performs steady-state thermal analysis. Make sure to click on this button a few minutes after clicking on the 'Start' button.
  * After doing the steady-state analysis you can start the transient-state analysis by pressing the `Transient` button. During the transient analysis, ThermTap produces the result every 1 second. Note that you can stop current transient analysis by clicking on the `Change PID/UID` button, and select another pid/uid to start the analysis with. Also note that the transient- or steady-state analysis is performed on the currently selected UID/PID. If nothing is selected, the analysis is done on the whole system.
    * To show the temperature maps, you can select your desired component name and layer from the device component panel, after performing the steady- or transient-state analysis. Note that in the case of transient analysis, you need to reselect the previously chosen layer (or any other component+layer combination) in order to referesh the temperature map. This gives you enough time to watch the current temperature map.


## Porting to a New Device
Please note that ThermTap is tuned to work with Google Nexus 5. If you want to port it to another device, the following actions are required:
  * A new device description file similar to `package_N5.xml` is created and used.
  * The kernel of the device is recompiled as explained in the Requirements section.
  * The SystemTap module is rebuilt as explained in the *Requirements* section.
  * The power models described in `ThermTap/PowerTap/src/edu/usc/powertap/powermodel/*.java` should be tuned again.


## Developers
* [Mohammad Javad Dousti](<dousti@usc.edu>)
* [Majid Ghasemi-Gol](<ghasemig@usc.edu>)
* [Mahdi Nazemi](<mnazemi@usc.edu>)
* [Massoud Pedram](<pedram@usc.edu>)

## Asking Questions or Reporting Bugs
You may contact [Mohammad Javad Dousti](<dousti@usc.edu>) for any questions you may have or bugs that you find.
