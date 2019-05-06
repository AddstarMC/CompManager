package au.com.addstar.comp.lobby.signs;

public class SignRefresher implements Runnable {
	private final SignManager manager;
	public SignRefresher(SignManager manager) {
		this.manager = manager;
	}
	
	@Override
	public void run() {
		for (BaseSign sign : manager.getAllSigns()) {
			sign.refresh();
		}
	}
}
