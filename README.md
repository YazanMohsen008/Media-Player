# Media Player 

* The goal of this application is to read media files (video, audio) form and play them
* This application is built using the Android framework but it doesn't use its libraries (except visualizing libraries) it works based on the standard of each supported format  
  ## Video Files
  * Read video files from the device.
  * Parse the file and extract the data which needs to be decoded. 
  * Decode the parsed file and transform it to an array of frames each frame is composed of pixels.
  * Draw the frames in sequence, with the right fps.
  * Supported Format (.mp4) 
  
  ## Audio Files
  * Read audio files from the device.
  * Parse the file and extract the data which needs to be decoded. 
  * Decode the parsed file and transform it to an array of Units.
  * Play the units in sequence.
  * Supported Format (.mp3,.wav) 
   
 #### Support Only Android Devices
