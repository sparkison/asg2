package cs455.harvester.thread;

public class ResultListener implements ListenerInterface<Object>{

	@Override
	public void finish(Object obj) {}

	@Override
	public void error(Exception ex) {
		ex.printStackTrace();
	}

}