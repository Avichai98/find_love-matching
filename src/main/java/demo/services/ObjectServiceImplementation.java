package demo.services;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import demo.ObjectId;
import demo.Role;
import demo.boundries.NewUserBoundary;
import demo.boundries.ObjectBoundary;
import demo.boundries.UserBoundary;
import demo.entities.UserId;
import demo.exception.BadRequest400;
import demo.exception.UnauthorizedAccess401;
import demo.exception.NotFound404;
import demo.interfaces.ObjectCrud;
import demo.interfaces.ObjectService;
import demo.interfaces.UserCrud;
import demo.interfaces.UserService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ObjectServiceImplementation implements ObjectService {

	private ObjectCrud objectCrud;
	private UserCrud userCrud;

	@Value("${spring.application.name}")
	private String superAppName;

	@Value("${helper.delimiter}")
	private String delimiter;

	public ObjectServiceImplementation(ObjectCrud objectCrud, UserCrud userCrud) {
		super();
		this.objectCrud = objectCrud;
		this.userCrud = userCrud;
	}

	@Override
	public Mono<ObjectBoundary> create(ObjectBoundary object) {
		if (object.getObjectId() == null) {
			object.setObjectId(new ObjectId());
		}
		object.getObjectId().setId(UUID.randomUUID().toString()).setSuperapp(superAppName);
		object.setCreationTimestamp(new Date());
		object.getCreatedBy().getUserId().setSuperapp(superAppName);

		if (object.getType().equals("Dreamer")) {
			String url = "http://localhost:8085/superapp/users";
			WebClient webClient = WebClient.create(url);
			
			//Create new UserBoundary
			NewUserBoundary nub = new NewUserBoundary();
			
			//Get data from objectDetails map
			String email = object.getCreatedBy().getUserId().getEmail();
			String username = object.getObjectDetails().containsKey("username") ? (String) object.getObjectDetails().get("username") : null;
			String avatar = object.getObjectDetails().containsKey("avatar") ? (String) object.getObjectDetails().get("avatar") : null;
			String location = object.getObjectDetails().containsKey("location") ? (String) object.getObjectDetails().get("location") : null;
			String birthday = object.getObjectDetails().containsKey("birthday") ? (String) object.getObjectDetails().get("birthday") : null;
			String gender = object.getObjectDetails().containsKey("gender") ? (String) object.getObjectDetails().get("gender") : null;
			if (email == null || email == "" || username == null || username == "" ||
					avatar == null || avatar == "" || location == null || location == "" ||
					birthday == null || birthday == "" || gender == null || gender == "") {
				return Mono.error(() -> new BadRequest400("Some needed attribute are null or empty"));
			}
			
			//Set NewUserBoundary
			nub.setEmail(email);
			nub.setRole(Role.SUPERAPP_USER);
			nub.setUsername(username);
			nub.setAvatar(avatar);
			
			//Post using WebClient
			 webClient.post().bodyValue(nub).retrieve()
	            .bodyToMono(UserBoundary.class)
	            .subscribe(
	                    userBoundary -> {
	                        // Handle the response here
	                        System.out.println("User created: " + userBoundary.getUsername());
	                    },
	                    throwable -> {
	                        // Handle errors here
	                        System.err.println("Error creating user: " + throwable.getMessage());
	                    }
	            );
		}
		
		else if(object.getType().equals("Counselor")) {
			String url = "http://localhost:8085/superapp/users";
			WebClient webClient = WebClient.create(url);
			
			//Create new UserBoundary
			NewUserBoundary nub = new NewUserBoundary();
			
			//Get data from objectDetails map
			String email = object.getCreatedBy().getUserId().getEmail();
			String username = object.getObjectDetails().containsKey("username") ? (String) object.getObjectDetails().get("username") : null;
			String avatar = object.getObjectDetails().containsKey("avatar") ? (String) object.getObjectDetails().get("avatar") : null;
			String birthday = object.getObjectDetails().containsKey("birthday") ? (String) object.getObjectDetails().get("birthday") : null;
			String phoneNumber = object.getObjectDetails().containsKey("phoneNumber") ? (String) object.getObjectDetails().get("phoneNumber") : null;
			Integer experience = object.getObjectDetails().containsKey("experience") ? (Integer) object.getObjectDetails().get("experience") : null;
			String specialization = object.getObjectDetails().containsKey("specialization") ? (String) object.getObjectDetails().get("specialization") : null;
			if (email == null || email == "" || username == null || username == "" ||
					avatar == null || avatar == "" || birthday == null || birthday == "" ||
					phoneNumber == null || phoneNumber == "" || experience == null ||
					specialization == null || specialization == "") {
				return Mono.error(() -> new BadRequest400("Some needed attribute are null or empty"));
			}
			
			//Set NewUserBoundary
			nub.setEmail(email);
			nub.setRole(Role.SUPERAPP_USER);
			nub.setUsername(username);
			nub.setAvatar(avatar);
			
			//Post using WebClient
			 webClient.post().bodyValue(nub).retrieve()
	            .bodyToMono(UserBoundary.class)
	            .subscribe(
	                    userBoundary -> {
	                        // Handle the response here
	                        System.out.println("User created: " + userBoundary.getUsername());
	                    },
	                    throwable -> {
	                        // Handle errors here
	                        System.err.println("Error creating user: " + throwable.getMessage());
	                    }
	            );
		}
		
			return Mono.just(object).flatMap(boundary -> {
				if ((boundary.getType() == null || boundary.getType().isEmpty()) || boundary.getObjectDetails() == null
						|| boundary.getCreatedBy() == null || boundary.getCreatedBy().getUserId() == null
						|| boundary.getCreatedBy().getUserId().getEmail() == null || boundary.getAlias() == null
						|| boundary.getAlias().isEmpty())
					return Mono.error(() -> new BadRequest400("Some needed attribute are null"));

				this.userCrud.findById(object.getCreatedBy().getUserId().getSuperapp() + delimiter
						+ object.getCreatedBy().getUserId().getEmail()).flatMap(user -> {
							if (user.getRole() != Role.SUPERAPP_USER) {
								return Mono
										.error(() -> new BadRequest400("You dont have permission to create object."));
							}
							return Mono.just(boundary);
						});
				return Mono.just(boundary);
			}).map(ObjectBoundary::toEntity).flatMap(this.objectCrud::save).map(entity -> new ObjectBoundary(entity))
					.log();
		}

	@Override
	public Mono<ObjectBoundary> getObject(String objectSuperapp, String id, String userSuperapp, String userEmail) {
		// find the user with this userSuperapp and userEmail.
		return this.userCrud.findById(userSuperapp + delimiter + userEmail)
				.switchIfEmpty(Mono.error(new NotFound404("User not found"))).flatMap(user -> {
					// check if he has permission to update objects (if his role is SUPERAPP_USER).
					if (user.getRole().equals(Role.SUPERAPP_USER)) {
						// return the object if found,
						// else return a NotFound message.
						return this.objectCrud.findById(objectSuperapp + delimiter + id)
								.switchIfEmpty(Mono.error(() -> new NotFound404("Object not found in database.")))
								.map(entity -> new ObjectBoundary(entity)).log();
					}
					// check if the user role is MINIAPP_USER.
					else if (user.getRole().equals(Role.MINIAPP_USER)) {
						// return the object if found and only if the Active attribute is True.
						// else return an empty Mono.
						return this.objectCrud.findByObjectIdAndActiveIsTrue(id).switchIfEmpty(Mono.empty())
								.map(entity -> new ObjectBoundary(entity)).log();
					}
					// other roles, return UnauthorizedAccess message.
					else {
						return Mono.error(() -> new UnauthorizedAccess401("You dont have permission to get objects."));
					}
				});
	}

	@Override
	public Flux<ObjectBoundary> getAllObjects(String userSuperapp, String userEmail) {
		// find the user with this userSuperapp and userEmail.
		return this.userCrud.findById(userSuperapp + ":" + userEmail)
				.switchIfEmpty(Mono.error(new NotFound404("User not found"))).flatMapMany(user -> {
					// check if he has permission to update objects (if his role is SUPERAPP_USER).
					if (user.getRole().equals(Role.SUPERAPP_USER)) {
						return this.objectCrud.findAll().map(entity -> new ObjectBoundary(entity)).log();
						// check if the user role is MINIAPP_USER.
					} else if (user.getRole().equals(Role.MINIAPP_USER)) {
						// return all objects with Active attribute is True.
						return this.objectCrud.findAllByActiveIsTrue().map(entity -> new ObjectBoundary(entity)).log();
					}
					// other roles, return UnauthorizedAccess message.
					else {
						return Flux.error(new UnauthorizedAccess401("You dont have permission to get objects."));
					}
				});
	}

	@Override
	public Mono<Void> updateObject(String objectSuperapp, String id, ObjectBoundary update, String userSuperapp,
			String userEmail) {
		// find the user with this userSuperapp and userEmail
		return this.userCrud.findById(userSuperapp + ":" + userEmail)
				.switchIfEmpty(Mono.error(new NotFound404("User not found"))).flatMap(user -> {
					// check if he has permission to update objects (if his role is SUPERAPP_USER).
					if (!user.getRole().equals(Role.SUPERAPP_USER)) {
						// return an UnauthorizedAccess message if the role is not SUPERAPP_USER.
						return Mono.error(() -> new UnauthorizedAccess401("You dont have permission to udpate."));
					} else {
						// update the needed object.
						return this.objectCrud.findById(objectSuperapp + delimiter + id).map(entity -> {
							entity.setActive(update.getActive());
							// check if type is null or empty string.
							if (update.getType() != null && update.getType() != "") {
								entity.setType(update.getType());
							}
							entity.setObjectDetails(update.getObjectDetails());
							// check if alias is null or empty string.
							if (update.getAlias() != null && update.getAlias() != "") {
								entity.setAlias(update.getAlias());
							}
							return entity;
						}).flatMap(this.objectCrud::save).map(ObjectBoundary::new).log().then();
					}

				});

	}

	@Override
	public Flux<ObjectBoundary> searchbyType(String type, String superApp, String userEmail) {

		return this.userCrud.findById(superApp + ":" + userEmail).flatMapMany(user -> {
			// Check if the user exists
			if (user != null) {
				// Check if the user is a MINIAPP_USER
				if (user.getRole().equals(Role.MINIAPP_USER)) {
					// Miniapp users can only search for objects associated with their miniapp
					return this.objectCrud.findAllByTypeAndActiveIsTrue(type).map(ObjectBoundary::new).log();
				}
				// Check if the user is a SUPERAPP_USER
				else if (user.getRole().equals(Role.SUPERAPP_USER)) {
					// Superapp users can search for all objects
					return this.objectCrud.findAllByType(type).map(ObjectBoundary::new).log();
				} else {
					// Unauthorized access for other roles
					return Flux.error(new UnauthorizedAccess401("You don't have permission to get objects."));
				}
			} else {
				// Return a NotFound error if the user is not found
				return Flux.error(new NotFound404("User not found"));
			}
		});
	}

	@Override
	public Flux<ObjectBoundary> searchbyAlias(String alias, String superApp, String userEmail) {
		return this.userCrud.findById(superApp + ":" + userEmail).flatMapMany(user -> {
			// Check if the user exists
			if (user != null) {
				// Check if the user is a MINIAPP_USER
				if (user.getRole().equals(Role.MINIAPP_USER)) {
					// Miniapp users can only search for objects associated with their miniapp
					return this.objectCrud.findAllByAliasAndActiveIsTrue(alias).map(ObjectBoundary::new).log();
				}
				// Check if the user is a SUPERAPP_USER
				else if (user.getRole().equals(Role.SUPERAPP_USER)) {
					// Superapp users can search for all objects
					return this.objectCrud.findAllByAlias(alias).map(ObjectBoundary::new).log();
				} else {
					// Unauthorized access for other roles
					return Flux.error(new UnauthorizedAccess401("You don't have permission to get objects."));
				}
			} else {
				// Return a NotFound error if the user is not found
				return Flux.error(new NotFound404("User not found"));
			}
		});
	}

	@Override
	public Flux<ObjectBoundary> searchbyAliasPattern(String pattern, String superApp, String userEmail) {
		return this.userCrud.findById(superApp + ":" + userEmail).flatMapMany(user -> {
			// Check if the user exists
			if (user != null) {
				// Check if the user is a MINIAPP_USER
				if (user.getRole().equals(Role.MINIAPP_USER)) {
					// Miniapp users can only search for objects associated with their miniapp
					return this.objectCrud.findAllByActiveIsTrueAndAliasLike("" + pattern + "").map(ObjectBoundary::new)
							.log();
				}
				// Check if the user is a SUPERAPP_USER
				else if (user.getRole().equals(Role.SUPERAPP_USER)) {
					// Superapp users can search for all objects
					return this.objectCrud.findAllByActiveIsTrueAndAliasLike(pattern).map(ObjectBoundary::new).log();
				} else {
					// Unauthorized access for other roles
					return Flux.error(new UnauthorizedAccess401("You don't have permission to get objects."));
				}
			} else {
				// Return a NotFound error if the user is not found
				return Flux.error(new NotFound404("User not found"));
			}
		});
	}

}
