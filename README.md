jconsoleserver
==============

Console Server in Java using apache Mina

This is a reimplementation of my console server that was originally written in Python. 

There are some details on www.klyne.org. (java-mina-ssh)

A console server is a terminal server working in reverse. Instead of connecting to a serial 
port with your terminal to talk to the computer, you connect over a network connection 
(SSH preferably) to talk to an external device (network router) on its serial management 
port. In the network world this can be out of band and allow you to access a device 
that is having problems.

Notes
=======

This is work in progress, the command interface is not fully implemented. 
Testing has been minimal but SSH access on 8000 will get the command interface and SSH access on ports in the consoleserver.ini file
will access com ports that are available.
The branch getmethod is start of re working the command interface to use reflection instead of classes for command implementation.


