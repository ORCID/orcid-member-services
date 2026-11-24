
package org.orcid.mp.user.data.migration;

import com.mongodb.client.MongoDatabase;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.orcid.mp.user.domain.User;
import org.orcid.mp.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.util.Optional;

@ChangeUnit(id = "fix-corrupted-duplicate-users", order = "002", author = "George Nash")
public class FixCorruptedBrokenUsers {

    private static final Logger LOG = LoggerFactory.getLogger(FixCorruptedBrokenUsers.class);

    @Execution
    public void execution(UserRepository userRepository, Environment environment) {
        String originalUserId = environment.getProperty("application.data.migration.corruptedUser.original");
        String duplicateUserId = environment.getProperty("application.data.migration.corruptedUser.duplicate");

        Optional<User> originalUserOpt = userRepository.findById(originalUserId);
        Optional<User> duplicateUserOpt = userRepository.findById(duplicateUserId);

        if (originalUserOpt.isEmpty() || duplicateUserOpt.isEmpty()) {
            LOG.warn("Could not find original user {} or duplicate user {}, skipping fix", originalUserId, duplicateUserId);
            return;
        }

        User originalUser = originalUserOpt.get();
        User duplicateUser = duplicateUserOpt.get();

        originalUser.setMemberId(duplicateUser.getMemberId());
        userRepository.save(originalUser);
        userRepository.delete(duplicateUser);

        LOG.info("Successfully set memberId {} on original user {} and deleted duplicate user {}",
                duplicateUser.getMemberId(), originalUserId, duplicateUserId);
    }

    @RollbackExecution
    public void rollbackExecution(MongoDatabase mongoDatabase) {
        LOG.warn("Rollback executed for fix-corrupted-duplicate-users, but the duplicate user deletion is irreversible.");
    }
}