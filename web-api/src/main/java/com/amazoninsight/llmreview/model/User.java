package com.amazoninsight.llmreview.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name="users")
@Data
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(min = 5, max = 50)
    @Column(unique = true)
    private String username;

    @NotNull
    @Email
    @Column(unique = true)
    private String email;

    @NotNull
    @Column(nullable = false)
    private String firstName;

    @NotNull
    @Column(nullable = false)
    private String lastName;

    @NotNull
    @Column(nullable = false)
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Column(nullable = false, updatable = false)
    private LocalDate creationDate;

    @Column(name = "account_non_locked", nullable = false)
    private boolean accountNonLocked;

    @NotNull
    @Column(nullable = false)
    private Boolean enabled;

    @Column(name = "failed_attempt", nullable = false)
    private int failedAttempt;

    @Column(name = "lock_time")
    private LocalDate lockTime;

    @Column(name = "activation_token", length = 64, nullable = false)
    private String activationToken;

    @Column(name = "activation_token_expiry", nullable = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDate activationTokenExpiry;

    @PrePersist
    protected void onCreate() {
        this.activationToken = UUID.randomUUID().toString();
        this.creationDate = LocalDate.now();
        this.enabled = false;
        this.accountNonLocked = true;
        this.activationTokenExpiry = this.creationDate.plusDays(1);
        this.failedAttempt = 0;
        this.lockTime = null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // add logic to manage this
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // add logic to manage this
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

}
