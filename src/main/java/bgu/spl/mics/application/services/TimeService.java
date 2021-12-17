package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.TerminationBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;

import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Thread.sleep;

/**
 * TimeService is the global system timer There is only one instance of this micro-service.
 * It keeps track of the amount of ticks passed since initialization and notifies
 * all other micro-services about the current time tick using {@link TickBroadcast}.
 * This class may not hold references for objects which it is not responsible for.
 * 
 * You can add private fields and public methods to this class.
 * You MAY change constructor signatures and even add new public constructors.
 */
public class TimeService extends MicroService{

	int speed; //num of millis each clock tick takes
	int duration; //num of ticks before termination
	int totalTicks;
	private Timer timer;

	public TimeService(int speed, int duration) {
		super("Time Service");
		totalTicks = 0;
		this.speed = speed;
		this.duration = duration;
		timer = new Timer(true);
	}

	@Override
	protected void initialize() {
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				sendBroadcast(new TickBroadcast());
				totalTicks = totalTicks + 1;

				if(totalTicks * speed == duration){
					sendBroadcast(new TerminationBroadcast());
					this.cancel();
				}
			}
		}, 0 , speed);

		subscribeBroadcast(TerminationBroadcast.class, c -> terminate());

	}

}
