package ai.netoai.collector.deviceprofile;

import java.util.ArrayList;
import java.util.List;

public class TrapConfig implements Config{

	private String id;
	private List<Trap> traps = new ArrayList<Trap>();
	private TrapSynchronize synchronize;
	
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	/**
	 * @return the traps
	 */
	public List<Trap> getTraps() {
		return traps;
	}
	/**
	 * @param traps the traps to set
	 */
	public void setTraps(List<Trap> traps) {
		this.traps = traps;
	}
	
	public TrapSynchronize getSynchronize() {
		return synchronize;
	}
	public void setSynchronize(TrapSynchronize synchronize) {
		this.synchronize = synchronize;
	}
	
}
