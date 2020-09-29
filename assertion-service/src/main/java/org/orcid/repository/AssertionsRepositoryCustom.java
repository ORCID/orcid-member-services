package org.orcid.repository;

import java.util.List;

import org.orcid.domain.Assertion;

public interface AssertionsRepositoryCustom {
	
	List<Assertion> findAllToUpdate();

}
