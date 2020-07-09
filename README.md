# 7s
singing web application

# Applet

Uses (TarsosDSP)[https://github.com/JorenSix/TarsosDSP] library for sound processing and pitch detection (download, unzip and add to build path).
Music, applauses and icons are not provided:

 - Notes and rythm corresponds to the beginning of the chorus of "Halleluja" by Leonard Cohen
 - Star icon is free use
 - applause is free use sound (sound recorded by Yannick Lemieux: (here)[https://www.youtube.com/watch?v=IlV1ZyNsS0s])

## What the applet does

Plays a music and provides the notes you need to sing (actually the pitch) in a synchronized way. The notes are represented by yellow bars moving from right to left. The synchronization with the music is represented by a white vertical bar.

The applet records your voice using he mic, suppresses the silence, and detects the pitch of your voice (signal to noise ratio shall be sufficient i.e. sing loud! Don't be shy). The note you are singing is represented by a star. Now you have to command the star by adapting your note so that the star is on top of the bars representing the notes to sing. When you are close enough you start to receive points!

Code includes the possibility to modulate, as an exercise to sing a third or a fifth above.

## What the applet does not do... yet

It would be possible also to modify the pitch of the music to adapt to the tessitura of the singer.

Finally, noise reduction algorithm using FFT filtering has not been implemented yet.

Load the music, store the score.

