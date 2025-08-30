package service;

import model.User;
import repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UserService {
    
    private final UserRepository userRepository;
    
    public UserService() {
        this.userRepository = new UserRepository();
    }
    
    

public User updateUser(User user) throws Exception {
    if (user == null || user.getId() == null) {
        throw new IllegalArgumentException("User and user ID cannot be null");
    }
    
    
    User existingUser = userRepository.findById(user.getId())
        .orElseThrow(() -> new Exception("User not found"));
    
    return userRepository.save(user);
}
    

public User registerUser(String username, String email, String password, 
                        String firstName, String lastName) throws Exception {
    
    if (username == null || username.trim().isEmpty()) {
        throw new IllegalArgumentException("Username cannot be empty");
    }
    if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
        throw new IllegalArgumentException("Invalid email format");
    }
    if (password == null || password.length() < 6) {
        throw new IllegalArgumentException("Password must be at least 6 characters long");
    }
    
    
    if (userRepository.existsByUsername(username)) {
        throw new Exception("Username already exists");
    }
    if (userRepository.existsByEmail(email)) {
        throw new Exception("Email already exists");
    }
    
    
    User user = new User(username, email, hashPassword(password));
    user.setVerificationToken(generateVerificationToken());
    
    
    if (firstName != null && !firstName.trim().isEmpty()) {
        user.setFirstName(firstName.trim());
    }
    if (lastName != null && !lastName.trim().isEmpty()) {
        user.setLastName(lastName.trim());
    }
    
    return userRepository.save(user);
}
    
    public Optional<User> login(String usernameOrEmail, String password) {
        
        Optional<User> user = userRepository.findByUsername(usernameOrEmail);

        
        
        if (user.isEmpty()) {
            user = userRepository.findByEmail(usernameOrEmail);
        }
        
        
        if (user.isPresent() && verifyPassword(password, user.get().getPasswordHash())) {
            return user;
        }
        
        return Optional.empty();
    }
    
    public User updateProfile(Long userId, String firstName, String lastName, String profileImage) throws Exception {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new Exception("User not found"));
        
        if (firstName != null) user.setFirstName(firstName);
        if (lastName != null) user.setLastName(lastName);
        if (profileImage != null) user.setProfileImage(profileImage);
        
        return userRepository.save(user);
    }
    
    public User changePassword(Long userId, String oldPassword, String newPassword) throws Exception {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new Exception("User not found"));
        
        if (!verifyPassword(oldPassword, user.getPasswordHash())) {
            throw new Exception("Invalid old password");
        }
        
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters long");
        }
        
        user.setPasswordHash(hashPassword(newPassword));
        return userRepository.save(user);
    }
    
    public User verifyEmail(String token) throws Exception {
        User user = userRepository.findByVerificationToken(token)
            .orElseThrow(() -> new Exception("Invalid verification token"));
        
        user.setIsVerified(true);
        user.setVerificationToken(null);
        
        return userRepository.save(user);
    }
    
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }
    
    public void deleteUser(Long userId) throws Exception {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new Exception("User not found"));
        userRepository.delete(user);
    }
    
    
    private String hashPassword(String password) {
        
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }
    
    private boolean verifyPassword(String password, String hashedPassword) {
        
        return BCrypt.checkpw(password, hashedPassword);
    }
    
    private String generateVerificationToken() {
        return UUID.randomUUID().toString();
    }
}