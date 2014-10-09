package com.mediaiqdigital.spring.acl.jpa.predicate;

import java.util.List;

import com.mediaiqdigital.spring.acl.jpa.domain.AclClass;
import com.mediaiqdigital.spring.acl.jpa.domain.AclObjectIdentity;
import com.mediaiqdigital.spring.acl.jpa.domain.AclSid;
import com.mediaiqdigital.spring.acl.jpa.domain.QAclClass;
import com.mediaiqdigital.spring.acl.jpa.domain.QAclEntry;
import com.mediaiqdigital.spring.acl.jpa.domain.QAclObjectIdentity;
import com.mediaiqdigital.spring.acl.jpa.domain.QAclSid;
import com.mysema.query.jpa.JPASubQuery;
import com.mysema.query.types.expr.BooleanExpression;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.query.ListSubQuery;
import com.mysema.query.types.query.NumberSubQuery;
import com.mysema.query.types.query.SimpleSubQuery;

public abstract class AclPredicate<T, Q extends EntityPathBase<T>> {

	protected abstract Class<T> getType();

	protected abstract NumberPath<Long> getObjectId();

	protected abstract Q getInnerObject();

	protected abstract NumberPath<Long> getInnerObjectId();

	private ListSubQuery<AclSid> selectAclSidSubQuery(List<String> sids) {
		JPASubQuery subquery = new JPASubQuery();
		QAclSid aclSid = QAclSid.aclSid;
		subquery.from(aclSid).where(aclSid.sid.in(sids));
		return subquery.list(aclSid);
	}

	private SimpleSubQuery<AclClass> selectAclClassSubQuery() {
		JPASubQuery subQuery = new JPASubQuery();
		QAclClass aclClass = QAclClass.aclClass;
		subQuery.from(aclClass).where(
				aclClass.className.eq(getType().getName()));
		return subQuery.unique(aclClass);
	}

	private SimpleSubQuery<AclObjectIdentity> selectAclObjectIdentity(
			NumberPath<Long> id) {
		QAclObjectIdentity objectIdentity = QAclObjectIdentity.aclObjectIdentity;
		return new JPASubQuery()
				.from(objectIdentity)
				.where(objectIdentity.objectIdIdentity.eq(id).and(
						objectIdentity.objectIdClass
								.eq(selectAclClassSubQuery())))
				.unique(objectIdentity);
	}

	private NumberSubQuery<Long> selectCountOfAclEntry(NumberPath<Long> id,
			List<String> sids) {
		QAclEntry aclEntry = QAclEntry.aclEntry;
		return new JPASubQuery()
				.from(aclEntry)
				.where(aclEntry.aclObjectIdentity.eq(
						selectAclObjectIdentity(id)).and(
						aclEntry.granting.eq(true).and(
								aclEntry.sid.in(selectAclSidSubQuery(sids)))))
				.count();
	}

	private ListSubQuery<Long> selectWhereSomeObjectHasAnAclEntry(
			NumberPath<Long> id, List<String> sids) {
		// QCustomer innerSomeObject = new QCustomer("innerSomeObject");
		return new JPASubQuery().from(getInnerObject())
				.where(selectCountOfAclEntry(id, sids).gt(0L))
				.list(getInnerObjectId());
	}

	/**
	 * Returns a predicate defining a query from {@code SomeObject} which
	 * performs the necessary check to ensure that the user has privileges to
	 * view the someObject in at least some form or another. This predicate is
	 * used for non global roles to determine if someObject should be displayed
	 * on the dashboard.
	 * 
	 * Runs a sub-query to determine all the id's that the user has permission
	 * to view and then does a simple check to figure out whether the current id
	 * is present in the set of viewable ids.
	 * 
	 * @param sids
	 *            - the sids for the user. These can be obtained from the
	 *            AuthUtil
	 * @return - the predicate
	 * @see AuthUtil#getAllSids()
	 */
	public BooleanExpression viewableFor(List<String> sids) {

		/**
		 * Equivalent to the following JPQL: select someObject from SomeObject
		 * someObject where someObject.someProperty = 'true' or someObject.id in
		 * (select innerSomeObject.id from SomeObject innerSomeObject where
		 * (select count(aclEntry) from AclEntry aclEntry where
		 * aclEntry.aclObjectIdentity = (select aclObjectIdentity from
		 * AclObjectIdentity aclObjectIdentity where
		 * aclObjectIdentity.objectIdIdentity = someObject.id and
		 * aclObjectIdentity.objectIdClass = (select aclClass from AclClass
		 * aclClass where aclClass.objectClass = 'com.acme.SomeObject')) and
		 * aclEntry.granting = 'true' and aclEntry.sid in (select aclSid from
		 * AclSid aclSid where aclSid.sid in (:sids))) > 0)
		 */
		// QCustomer someObject = QCustomer.customer;
		return getObjectId().in(
				selectWhereSomeObjectHasAnAclEntry(getObjectId(), sids));
	}
}
