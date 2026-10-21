package com.shoplab.auth.security;

import com.shoplab.auth.repo.UserRepository;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JdbcUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var u = users.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(username));
        var authorities = u.roles().stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).collect(Collectors.toSet());
        return new org.springframework.security.core.userdetails.User(u.username(), u.passwordHash(),
                u.active(), true, true, true, authorities);
    }
}
