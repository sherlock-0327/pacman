package pacman;

/*only permit one loader*/
public class MyClassLoader extends ClassLoader {
	static public MyClassLoader singletonInstance = null;

	MyClassLoader() {

	}

	static public MyClassLoader getSingleInstance() {
		if (singletonInstance == null) {
			singletonInstance = new MyClassLoader();
		}
		return singletonInstance;
	}
}
