package au.com.addstar.comp;

import java.util.List;

import com.google.common.collect.Lists;

import au.com.addstar.comp.criterions.BaseCriterion;
import au.com.addstar.comp.prizes.BasePrize;

/**
 * Represents a competition. This may be a current competition, or a past one
 */
public class Competition {
	private int compId;
	private CompState state;
	
	private String theme;
	private List<BaseCriterion> criteria;
	
	private long startDate;
	private long endDate;
	private long voteEndDate;
	private int maxEntrants;
	
	private BasePrize firstPrize;
	private BasePrize secondPrize;
	private BasePrize participationPrize;

	private String votingStrategy;
	
	public Competition() {
		criteria = Lists.newArrayList();
		compId = -1;
	}
	
	public int getCompId() {
		return compId;
	}
	
	public void setCompId(int id) {
		compId = id;
	}
	
	/**
	 * Gets the current state of this competition
	 * @return One of CompState
	 */
	public CompState getState() {
		if (state != null) {
			return state;
		}
		
		if (System.currentTimeMillis() < startDate) {
			return CompState.Closed;
		} else if (System.currentTimeMillis() < endDate) {
			return CompState.Open;
		} else if (System.currentTimeMillis() < voteEndDate) {
			return CompState.Voting;
		} else {
			return CompState.Closed;
		}
	}
	
	/**
	 * Checks if this comp is running automatically
	 * @return True if state will change based on time
	 */
	public boolean isAutomatic() {
		return state == null;
	}
	
	/**
	 * Overrides the state of this comp.
	 * When set, this comp will not automatically update the state
	 * @param state The overriding state
	 * @see #setAutoState()
	 */
	public void setState(CompState state) {
		this.state = state;
	}
	
	/**
	 * Sets the state to be automatically determined based on time
	 * @see #setState(CompState)
	 */
	public void setAutoState() {
		this.state = null;
	}
	
	public String getTheme() {
		return theme;
	}
	
	public void setTheme(String theme) {
		this.theme = theme;
	}
	
	public List<BaseCriterion> getCriteria() {
		return criteria;
	}
	
	public long getStartDate() {
		return startDate;
	}
	
	public void setStartDate(long startDate) {
		this.startDate = startDate;
	}
	
	public long getEndDate() {
		return endDate;
	}
	
	public void setEndDate(long endDate) {
		this.endDate = endDate;
	}
	
	public long getVoteEndDate() {
		return voteEndDate;
	}
	
	public void setVoteEndDate(long endDate) {
		voteEndDate = endDate;
	}
	
	public int getMaxEntrants() {
		return maxEntrants;
	}
	
	public void setMaxEntrants(int maxEntrants) {
		this.maxEntrants = maxEntrants;
	}
	
	public BasePrize getFirstPrize() {
		return firstPrize;
	}
	
	public void setFirstPrize(BasePrize prize) {
		firstPrize = prize;
	}
	
	public BasePrize getSecondPrize() {
		return secondPrize;
	}
	
	public void setSecondPrize(BasePrize prize) {
		secondPrize = prize;
	}
	
	public BasePrize getParticipationPrize() {
		return participationPrize;
	}
	
	public void setParticipationPrize(BasePrize prize) {
		participationPrize = prize;
	}
	
	public boolean isRunning() {
		return getState() == CompState.Open;
	}

	public String getVotingStrategy() {
		return votingStrategy;
	}

	public void setVotingStrategy(String strategy) {
		this.votingStrategy = strategy;
	}
}
