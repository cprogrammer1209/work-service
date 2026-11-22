package com.projecty.work.modules.access.internal.repository;

import com.projecty.work.modules.access.internal.entity.MemberAccess;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberAccessRepository extends JpaRepository<MemberAccess, MemberAccess.Key> {

}
