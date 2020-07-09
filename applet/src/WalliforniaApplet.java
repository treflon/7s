import java.applet.Applet;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.SilenceDetector;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
import be.tarsos.dsp.util.PitchConverter;

/**
 * Uses TarsosDSP from University Ghent, IPEM.
 * Sounds are not embedded.
 */

/**
 * @author Développements
 *
 */
public class WalliforniaApplet extends Applet implements PitchDetectionHandler, AudioProcessor {

	private static long startApplauseOffset = 100000; ///< How much time BEFORE the end of the sound the applause sound starts
	private static String applauseFile = "D:\\Users\\Développements\\Downloads\\SMALL_CROWD_APPLAUSE-Yannick_Lemieux-1268806408-44100.wav";
	private static String songFile = "D:\\Users\\Développements\\Downloads\\3sc7d-2xr92.wav";

	private BufferedImage bufferedImage;
	private Graphics2D bufferedGraphics;
	private AudioDispatcher dispatcher;

	private float sampleRate = 44100;
	private int bufferSize = 1024 * 4;
	private int overlap = 768 * 4 ;

	private Mixer currentMixer;	
	private PitchEstimationAlgorithm algo;
	private double pitch;
	private double previousPitch;
	private int score;

	private static double[] notesNormal = {165, 196, 220, 220, 196, 165};
	private static double[] notesTierce = {196, 208, 233, 233, 208, 196};

	// Exercice de chanter à la tierce ou la quinte
	/** Number of half tones to pitch
	 *
	 * Positive value=pitch up, Negative value=pitch down
	 */
	int halfTones = 0; // 0 inchangé, 7 pour une quinte, 3 pour une tierce mineure, 4 pour une tierce majeure, etc... Le chant n'est pas modifié pour le moment...
	double ratio = 196.0/130.81;

	double[] notes =     {165,          196,                220,                    220,                  196,                  165};
	double[] start =     {1.0e3,        1.0e3+60.0e3/100.0, 1.0e3+1.5*60.0e3/100.0, 1.0e3+5*60.0e3/100.0, 1.0e3+6*60.0e3/100.0, 1.0e3+6.5*60.0e3/100.0};
	double[] durations = {60.0e3/100.0, 30.0e3/100.0,       2.0*90.0e3/100.0,       60.0e3/100.0,         30.0e3/100.0,         2.0*90.0e3/100.0};
	private SilenceDetector silenceDetector;

	public WalliforniaApplet() {
		System.out.println(ratio);
		System.out.println(Math.pow(2, 7.0/12.0));
		algo = PitchEstimationAlgorithm.DYNAMIC_WAVELET;
		if(halfTones != 0) {
			double ratio;
			if(halfTones>0) {
				ratio = Math.pow(2, (double) halfTones/12.0);
			}
			else {
				ratio = 1/Math.pow(2, (double) -halfTones/12.0);
			}
			for(int i=0;i<notes.length;i++) {
				notes[i] *= ratio;
			}
		}
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub
		super.init();
		bufferedImage = new BufferedImage(640*4,480*4, BufferedImage.TYPE_INT_RGB);
		bufferedGraphics = bufferedImage.createGraphics();
		try {
			setNewMixer(AudioSystem.getMixer(Shared.getMixerInfo(false, true).elementAt(1)));
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Clip songClip;
	private Clip applauseClip;

	private BufferedImage starImg;

	@Override
	public void start() {
		// TODO Use a button to start?
		super.start();
		// Start the song
		try {
		    starImg = ImageIO.read(new File("D:\\Users\\Développements\\Pictures\\star-35788_32.png"));
			songClip = AudioSystem.getClip();
			AudioInputStream inputStream = AudioSystem.getAudioInputStream(new File(songFile));
			songClip.open(inputStream);
	        songClip.start();
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		super.stop();
	}

	String currentPitch = "";
	long startTime = -1;

	@Override
	public void paint(Graphics g) {
		// TODO Auto-generated method stub
		super.paint(g);
		bufferedGraphics.clearRect(0,0,getWidth(),getHeight());
		//long time = clip.getMicrosecondPosition()/1000;
		long time = System.currentTimeMillis();
		if(startTime<0) {
			startTime = time;
		}
		time -= startTime;
		// TODO Compute tolerance. Tolerance depends on dificulty and level of user. sigma for the score should be linked to tolerance.
		int tolerance = (int) (getHeight()*0.025);
		//System.out.println(tolerance);
		// 0 start=1 => droite (getWidth) start=0 => milieu
		// 1 start=0 => gauche(0) start=1 => milieu start=2 => droite(getWidth)
		// 2 start=1 => gauche(0) start=2 => milieu start=3 => droite(getWidth)
		// getWidth()*(start-time+1)/2
        int pitchIndex = frequencyToBin(pitch);
		for(int i=0;i<notes.length;i++) {
			if(time+1e3>=start[i]) {
				int noteIndex = frequencyToBin(notes[i]);
				if((noteIndex-tolerance/2 <= pitchIndex) && (pitchIndex <= noteIndex+tolerance/2)) {
					bufferedGraphics.setColor(Color.GREEN);
				}
				else {
	            	bufferedGraphics.setColor(Color.YELLOW);
				}
				bufferedGraphics.fillRect((int)(((start[i]-time+1.0e3)/2000.0)*getWidth()), noteIndex-tolerance/2, (int)((durations[i]/2000.0)*getWidth()), tolerance);
			}
		}

		currentPitch = new StringBuilder("Score: ").append((int) score).toString();
        bufferedGraphics.setColor(Color.WHITE);
		// bufferedGraphics.drawLine(getWidth()/2, 0, getWidth()/2, getHeight()); // thinken the line!
        bufferedGraphics.fillRect(getWidth()/2, 0, 2, getHeight()); // thinken the line!
        bufferedGraphics.drawString(currentPitch, 20, 20);

		if (pitch != -1) {
			// TODO Replace with cute icon (heart, star, circle, what ever...)
			// TODO "Shiny" effects
            //bufferedGraphics.setColor(Color.RED);
            //bufferedGraphics.fillRect(getWidth()/2-3, pitchIndex-3, 7, 7);
            bufferedGraphics.drawImage(starImg, getWidth()/2-16, pitchIndex-16, null);
            if(previousPitch>0 && ((pitch/previousPitch<0.99) || (pitch/previousPitch>1.01)))
            	System.out.println(pitch/previousPitch);
            previousPitch = pitch;
		}
		else {
			if(previousPitch>0) {
				pitchIndex = frequencyToBin(previousPitch);
	            bufferedGraphics.drawImage(starImg, getWidth()/2-16, pitchIndex-16, null);
	            previousPitch = -previousPitch;
			}
			else {
				pitchIndex = frequencyToBin(-previousPitch);
				bufferedGraphics.drawImage(starImg, getWidth()/2-8, pitchIndex-8, 16, 16, null);
			}
		}
        g.drawImage(bufferedImage, 0, 0, null);
        pitch = -1;
	}

	private void setNewMixer(Mixer mixer) throws LineUnavailableException, UnsupportedAudioFileException {

		if(dispatcher!= null){
			dispatcher.stop();
		}
		final AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
		final DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, format);
		TargetDataLine line;
		line = (TargetDataLine) mixer.getLine(dataLineInfo);
		final int numberOfSamples = bufferSize;
		line.open(format, numberOfSamples);
		line.start();
		final AudioInputStream stream = new AudioInputStream(line);

		JVMAudioInputStream audioStream = new JVMAudioInputStream(stream);
		// create a new dispatcher
		dispatcher = new AudioDispatcher(audioStream, bufferSize,
				overlap);
		currentMixer = mixer;

		silenceDetector = new SilenceDetector(-36,false);
		dispatcher.addAudioProcessor(silenceDetector);
		// add a processor, handle pitch event.
		dispatcher.addAudioProcessor(this);
		dispatcher.addAudioProcessor(new PitchProcessor(algo, sampleRate, bufferSize, this));

		// run the dispatcher (on a new thread).
		new Thread(dispatcher,"Audio dispatching").start();
	}

	@Override
	public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
		long time = songClip.getMicrosecondPosition()/1000;
		double newPitch = pitchDetectionResult.getPitch();
		if(pitchDetectionResult.isPitched() && (newPitch<=1000) && (newPitch>=55)){
			// TODO Try to remove anomalies!!!
			pitch = pitchDetectionResult.getPitch();
		} else {
			pitch = -1;
		}
		// Find note to sing
		for(int i=0;i<notes.length;i++) {
			if ((time>=start[i]) && (time<=start[i]+durations[i])) {
				if(pitch == -1) break;

				double deuxSigma = 0.05;
//				System.out.println(notes[i]+" vs. "+pitch+" => "+Math.abs(Math.log(notes[i]/pitch))+" "+Math.exp(-Math.abs((Math.log(notes[i]/pitch))/deuxSigma))+" "+Math.exp((Math.log(notes[i]/pitch))/deuxSigma));
				score += 1000.0*Math.exp(-Math.abs((Math.log(notes[i]/pitch))/deuxSigma));
				// TODO Depend on previous pitch to avoid higher update rate to gain more points
				break;
			}
		}

		this.repaint();
		if ((applauseClip == null) && (songClip.getMicrosecondLength() - songClip.getMicrosecondPosition() < startApplauseOffset)) {
			// TODO
			try {
				applauseClip = AudioSystem.getClip();
				AudioInputStream inputStream = AudioSystem.getAudioInputStream(
					new File(applauseFile));
				applauseClip.open(inputStream);
				applauseClip.start();
			} catch (LineUnavailableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedAudioFileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean process(AudioEvent audioEvent) {
		if(silenceDetector.currentSPL() < -37.0){
			pitch = -1;
			this.repaint();
			return false;
		}
		return true;
	}

	@Override
	public void processingFinished() {
		// TODO Auto-generated method stub
		
	} 
	 private int frequencyToBin(final double frequency) {
	        final double minFrequency = 55; // Hz
	        final double maxFrequency = 300; // Hz
	        int bin = 0;
	        final boolean logaritmic = true;
	        if (frequency != 0 && frequency > minFrequency && frequency < maxFrequency) {
	            double binEstimate = 0;
	            if (logaritmic) {
	                final double minCent = PitchConverter.hertzToAbsoluteCent(minFrequency);
	                final double maxCent = PitchConverter.hertzToAbsoluteCent(maxFrequency);
	                final double absCent = PitchConverter.hertzToAbsoluteCent(frequency * 2);
	                binEstimate = (absCent - minCent) / maxCent * getHeight();
	            } else {
	                binEstimate = (frequency - minFrequency) / maxFrequency * getHeight();
	            }
	            if (binEstimate > 700) {
	                System.out.println(binEstimate + "");
	            }
	            bin = getHeight() - 1 - (int) binEstimate;
	        }
	        return bin;
	    }

	 public void update(Graphics g) {
          paint(g);
     }

}
