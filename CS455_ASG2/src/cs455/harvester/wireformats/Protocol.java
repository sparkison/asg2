package cs455.harvester.wireformats;

public interface Protocol {
	final static int CRAWLER_SENDS_TASK_COMPLETE 		= 1;
	final static int CRAWLER_SENDS_TASK					= 2;
	final static int CRAWLER_SENDS_FINISHED				= 3;
	final static int CRAWLER_SENDS_INCOMPLETE			= 4;
}