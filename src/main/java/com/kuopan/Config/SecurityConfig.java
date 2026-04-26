package com.kuopan.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/checkCode", "/sendEmailCode", "/resetPassword", "/login",
                        "/getUsedSpace", "/updatePassword", "/exit",
                        "/file/**", "/recycle/**", "/share/**",
                        "/admin/**", "/showShare/**")
                .permitAll()
                .anyRequest().
                authenticated().and().csrf().disable();

        return http.build();
    }
}
