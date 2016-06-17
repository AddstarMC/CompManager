package au.com.addstar.comp.confirmations;

import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;

/**
 * Represents something to be confirmed
 * @param <T> The type of the handler
 */
public class Confirmation<T extends Confirmable> {
	private final T handler;
	private long expireTime;
	private String expireMessage;
	private String acceptMessage;
	private String abortMessage;
	
	private String tokenFailMessage;
	private String token;
	
	private Confirmation(T handler) {
		this.handler = handler;
	}
	
	public T getHandler() {
		return handler;
	}
	
	public long getExpireTime() {
		return expireTime;
	}
	
	public String getExpireMessage() {
		return expireMessage;
	}
	
	public String getAcceptMessage() {
		return acceptMessage;
	}
	
	public String getAbortMessage() {
		return abortMessage;
	}
	
	public String getToken() {
		return token;
	}
	
	public String getTokenFailMessage() {
		return tokenFailMessage;
	}
	
	/**
	 * Creates a {@link Builder} to make new Confirmations
	 * @param handler The handler for the confirmation
	 * @return a {@link Builder}
	 */
	public static <T extends Confirmable> Builder<T> builder(T handler) {
		return new Builder<>(handler);
	}
	
	public static class Builder<T extends Confirmable> {
		private final T handler;
		
		private Builder(T handler) {
			this.handler = handler;
		}
		
		private long expireIn = -1;
		
		/**
		 * <b>required</b>. Sets the expiry of this confirmation to be now + time.
		 * Only one of {@link #expiresIn(long, TimeUnit)} or {@link #expiresAt(long)} can be set.
		 * @param time The time in {@code unit} before expiry
		 * @param unit The TimeUnit for {@code time}
		 * @return this for chaining
		 */
		public Builder<T> expiresIn(long time, TimeUnit unit) {
			expireIn = unit.toMillis(time);
			expireAt = -1;
			return this;
		}
		
		private long expireAt = -1;
		/**
		 * <b>required</b>. Sets the expiry of this confirmation to be at time.
		 * Only one of {@link #expiresIn(long, TimeUnit)} or {@link #expiresAt(long)} can be set.
		 * @param time The unix time (in ms) to expire at
		 * @return this for chaining
		 */
		public Builder<T> expiresAt(long time) {
			expireAt = time;
			expireIn = -1;
			return this;
		}

		private String expireMessage;
		/**
		 * Sets the message that will be shown if the confirmation times out
		 * @param message The message to show
		 * @return this for chaining
		 */
		public Builder<T> withExpireMessage(String message) {
			expireMessage = message;
			return this;
		}
		
		private String acceptMessage;
		/**
		 * Sets the message that will be shown if the confirmation is accepted
		 * @param message The message to show
		 * @return this for chaining
		 */
		public Builder<T> withAcceptMessage(String message) {
			acceptMessage = message;
			return this;
		}
		
		private String abortMessage;
		/**
		 * Sets the message that will be shown if the confirmation is rejected
		 * @param message The message to show
		 * @return this for chaining
		 */
		public Builder<T> withAbortMessage(String message) {
			abortMessage = message;
			return this;
		}
		
		private String tokenFailMessage;
		/**
		 * Sets the message that will be shown if the token is not correct / provided
		 * @param message The message to show
		 * @return this for chaining
		 */
		public Builder<T> withTokenFailMessage(String message) {
			tokenFailMessage = message;
			return this;
		}
		
		private String token;
		/**
		 * Sets a required token the player must input to accept the confirmation 
		 * @param token Any string that the player must enter. NOTE: Spaces will be minimized
		 * @return this for chaining
		 */
		public Builder<T> withRequiredToken(String token) {
			this.token = token;
			return this;
		}
		
		/**
		 * Creates the confirmation with the settings you set
		 * @return A Confirmation object
		 * @throws IllegalStateException Thrown if you havent set the required options
		 */
		public Confirmation<T> build() throws IllegalStateException {
			Preconditions.checkState(expireAt != -1 || expireIn != -1, "An expiry time must be set");
			
			Confirmation<T> confirmation = new Confirmation<>(handler);
			if (expireAt != -1) {
				confirmation.expireTime = expireAt;
			} else {
				confirmation.expireTime = System.currentTimeMillis() + expireIn;
			}
			
			confirmation.expireMessage = expireMessage;
			confirmation.acceptMessage = acceptMessage;
			confirmation.abortMessage = abortMessage;
			confirmation.tokenFailMessage = tokenFailMessage;
			confirmation.token = token;
			
			return confirmation;
		}
	}
}