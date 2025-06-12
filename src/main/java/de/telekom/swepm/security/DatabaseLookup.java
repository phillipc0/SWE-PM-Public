package de.telekom.swepm.security;

import de.telekom.swepm.utils.UserUtils;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class DatabaseLookup implements UserDetailsService {
    @Autowired
    private UserUtils userUtils;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        val user = userUtils.getUser(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found.");
        }
        val authority = Set.of(new SimpleGrantedAuthority(user.getRole().name()));

        return new User(username.toLowerCase().replace(" ", ""), user.getPasswordHash(), authority);
    }

    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder(8);
    }
}
