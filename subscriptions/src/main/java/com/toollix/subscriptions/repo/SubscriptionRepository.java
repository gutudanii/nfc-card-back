package com.toollix.subscriptions.repo;

import com.toollix.subscriptions.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByTxRef(String txRef);

    List<Subscription> findBySubjectIdOrderByIdDesc(Long subjectId);

    List<Subscription> findBySubjectTypeAndSubjectIdOrderByIdDesc(String subjectType, Long subjectId);
}
