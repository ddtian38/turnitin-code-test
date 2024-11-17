package integrations.turnitin.com.membersearcher.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import integrations.turnitin.com.membersearcher.client.MembershipBackendClient;
import integrations.turnitin.com.membersearcher.exception.ClientRequestException;
import integrations.turnitin.com.membersearcher.model.MembershipList;
import integrations.turnitin.com.membersearcher.model.User;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MembershipService {
	@Autowired
	private MembershipBackendClient membershipBackendClient;

	private Map<String, User> userDetailsCache;

	/**
	 * Method to fetch all memberships with their associated user details included.
	 * The method calls out to the php-backend service and fetches all memberships.
	 * Before calling the php-backend service to get all memberships, this method checks and loads all user details to userDetailsCache object 
	 * from "/api.php/users" if there's no user details present. After it calls "/api.php/members" to fetch all memberships and then references userDetailsCache object
	 * to fetch the user details for each user individually and associates them with their corresponding membership.
	 *
	 * @return A CompletableFuture containing a fully populated MembershipList object.
	 */

	public CompletableFuture<MembershipList> fetchAllMembershipsWithUsers() {

		if(this.userDetailsCache == null){
			this.userDetailsCache = createUserCache();
		}

		return membershipBackendClient.fetchMemberships()
				.thenApply(members -> {
					members.getMemberships().forEach( member -> {
						if(userDetailsCache.containsKey(member.getUserId()))
							member.setUser(userDetailsCache.get(member.getUserId()));
					});

					return members;
				});
	}

	/**
	 * Method to fetch all user details from php backende service and stores them into HashMap to be used as a cache.
	 */
	private Map<String, User> createUserCache(){
		Map<String, User> userMap = new HashMap<>();
		try{
			membershipBackendClient.fetchUsers().get().getUsers().forEach( user -> {
				userMap.put(user.getId(), user);
			});
		}catch (ExecutionException | InterruptedException | RuntimeException e) {
			throw new ClientRequestException("Error creating User Cache: " + e.getMessage(), e);
		}
		return userMap;

	}
}
