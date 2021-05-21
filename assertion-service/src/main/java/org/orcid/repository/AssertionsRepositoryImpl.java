package org.orcid.repository;

import java.util.List;

import org.bson.Document;
import org.orcid.domain.Assertion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

public class AssertionsRepositoryImpl implements AssertionsRepositoryCustom {

	@Autowired
	MongoTemplate mongoTemplate;

	@Override
	public List<Assertion> findAllToUpdate() {

		Criteria dateCompareCriteria = new Criteria() {
			@Override
			public Document getCriteriaObject() {
				Document doc = new Document();
				doc.put("$where", "this.modified>this.updated_in_orcid");
				return doc;
			}
		};

		Query query = new Query().addCriteria(Criteria.where("updated").is(true));
		query.addCriteria(Criteria.where("put_code").ne(null));
		Criteria orCriteria = new Criteria();
		orCriteria.orOperator(Criteria.where("updated_in_orcid").is(null), dateCompareCriteria);
		query.addCriteria(orCriteria);

		return mongoTemplate.find(query, Assertion.class);
	}

}
