# Ajiranet graph path

## Technologies used
Java 8, 
Servlet


## Setup

I used eclipse.  Right click project, "Run as" -> "Run on Server"
This would run the application. 

Alternatively, you can generate war out of the given project.
In eclipse, right click project, "Export" -> war file 
Place war under server like tomcat.

use curl to invoke application api.



## Addendum

 - Introduced a command RESET, which is handy to clean the data.
 - FETCH /devices/detail 
is added to display type, name, strength and adjacent devices of each device.  This is handy to observe all details of the devices
 -  In windows, I had to use double quotes after --data, instead of single quotes
               

> curl --request POST --url %URL%  ^
>               --data "FETCH /devices"

   Things became messy with lot more mixing of single and double quotes.
   Tried using cygwin and that was working like gem.

## Design

Gateway.java is the api controller.

A Graph object is defined as singleton.  It holds all the nodes and connections.  Operations are performed over this object.

Json parsing class does all input data parsing.

The service class does semantic analysis of parsed input.  Invokes appropriate modules.

As per the problem statement, afaik, path between devices is expected.  No explicit requirement for shortest path.  However, in the test spec, from=A4&to=A3 expects shortest path.  So, implemented shortest path logic.

## Areas of improvements

In a real project, I would consider these aspects.

 - The input data are dumped into a complex hashMap.  Instead, I would create an object to hold all info about the input details.
 - Using json lib is a must.  Here I managed to parse the input based on the given test spec. Not sure if all constrains, input variations, error conditions are covered.
 - An operation should be within a transaction.  Rollback of entire operation should be possible.  In current model, for eg, We create connections - there is source device and array of targets.  What if third target is faulty and we throw exception?  What about first two targets?  They will be connected and rest of the targets are not.  As of now, we don't have rollback feature.
 - Good to have SAVE and LOAD option to serialize graph content into a file.


