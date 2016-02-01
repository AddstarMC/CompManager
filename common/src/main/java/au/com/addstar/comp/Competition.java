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
	
	private BasePrize firstPrize;
	private BasePrize secondPrize;
	private BasePrize participationPrize;
	
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
	
	public CompState getState() {
		return state;
	}
	
	public void setState(CompState state) {
		this.state = state;
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
		return (state == CompState.Open && System.currentTimeMillis() > startDate && System.currentTimeMillis() < endDate);
	}
}
