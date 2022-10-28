# IpCameraIntercom
An Android app that can turns certain IP cameras into intercoms with 2-way audio functionality. It uses Hikvision's Integrated Security API aka ISAPI to do this. This reuired some mofifications to the okhttp library so the underlying tcp/ip connection could be accessed. 

This is from the work in this project: https://blog.speedfox.co.uk/articles/1666958513-ip_camera_intercom/

## Building and using the app
Just import the project into intellij or build on the command line like any other android app

You will need to update cameraSettings.xml with the settings for your camera. Also to use this out of the box you will probably need to update your camera's settings to use 16bit PCM audio at 8000 samples/second. See the blog post above for the exact settings. 

## Cameras tested with 
* HikVision DS-2CD2145FWD-IS

Please email me if you get this to work with another camera and I'll update this list. 
