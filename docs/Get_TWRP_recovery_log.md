# Steps to get recovery log from TWRP

1. Open TWRP  
2. Flash Migrate zip. DO NOT REBOOT!!!  
3. In case of error go to TWRP main menu -> Advanced -> Copy Log.  

### Alternative 1:
Use TWRP File explorer.  
Navigate to "/tmp". Copy the "recovery.log" file to under "/sdcard".  

### Alternatively 2:
From adb, type this command:  
```
"cp /tmp/recovery.log /sdcard/"
```

### Finally:
Reboot to system and upload the "recovery.log" file, from Internal storage.