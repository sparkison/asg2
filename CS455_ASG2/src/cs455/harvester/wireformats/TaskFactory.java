package cs455.harvester.wireformats;

public class TaskFactory {
	// Singleton instance
		private static TaskFactory instance = null;

		// Exists only to defeat instantiation
		protected TaskFactory() {
		}

		// Get instance of TaskFactory
		public static TaskFactory getInstance() {
			if (instance == null) {
				instance = new TaskFactory();
			}
			return instance;
		}
}
