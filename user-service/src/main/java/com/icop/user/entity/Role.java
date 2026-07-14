package com.icop.user.entity;

// two roles is all this needs for now. stored by name (see User.role),
// so adding entries here later is safe — just don't rename existing ones
public enum Role {
    USER, ADMIN
}
