//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package play.db.jpa;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import play.db.DBApi;
import play.inject.ApplicationLifecycle;
import play.libs.F;
import play.libs.F.Promise;

public class DefaultJPAApi implements JPAApi {
private final JPAConfig jpaConfig;
private final Map<String, EntityManagerFactory> emfs = new HashMap();

public DefaultJPAApi(JPAConfig var1) {
this.jpaConfig = var1;
}

public JPAApi start() {
Iterator var1 = this.jpaConfig.persistenceUnits().iterator();

while(var1.hasNext()) {
JPAConfig.PersistenceUnit var2 = (JPAConfig.PersistenceUnit)var1.next();
this.emfs.put(var2.name, Persistence.createEntityManagerFactory(var2.unitName));
}

return this;
}

public EntityManager em(String var1) {
EntityManagerFactory var2 = (EntityManagerFactory)this.emfs.get(var1);
return var2 == null ? null : var2.createEntityManager();
}

public <T> T withTransaction(F.Function0<T> var1) throws Throwable {
return this.withTransaction("default", false, var1);
}

/** @deprecated */
@Deprecated
public <T> F.Promise<T> withTransactionAsync(F.Function0<F.Promise<T>> var1) throws Throwable {
return this.withTransactionAsync("default", false, var1);
}

public void withTransaction(F.Callback0 var1) {
try {
this.withTransaction("default", false, () -> {
var1.invoke();
return null;
});
} catch (Throwable var3) {
throw new RuntimeException("JPA transaction failed", var3);
}
}

public <T> T withTransaction(String var1, boolean var2, F.Function0<T> var3) throws Throwable {
EntityManager var4 = null;
EntityTransaction var5 = null;

Object var7;
try {
var4 = this.em(var1);
if (var4 == null) {
throw new RuntimeException("No JPA entity manager defined for '" + var1 + "'");
}

JPA.bindForSync(var4);
if (!var2) {
var5 = var4.getTransaction();
var5.begin();
}

Object var6 = var3.apply();
if (var5 != null) {
if (var5.getRollbackOnly()) {
var5.rollback();
} else {
var5.commit();
}
}

var7 = var6;
} catch (Throwable var13) {
if (var5 != null) {
try {
var5.rollback();
} catch (Throwable var12) {
}
}

throw var13;
} finally {
JPA.bindForSync((EntityManager)null);
if (var4 != null) {
var4.close();
}

}

return var7;
}

/** @deprecated */
@Deprecated
public <T> F.Promise<T> withTransactionAsync(String var1, boolean var2, F.Function0<F.Promise<T>> var3) throws Throwable {
EntityManager var4 = null;
EntityTransaction var5 = null;

try {
var4 = this.em(var1);
if (var4 == null) {
throw new RuntimeException("No JPA entity manager defined for '" + var1 + "'");
} else {
JPA.bindForAsync(var4);
if (!var2) {
var5 = var4.getTransaction();
var5.begin();
}

F.Promise var6 = (F.Promise)var3.apply();
F.Promise var9 = var5 == null ? var6 : var6.map((var1x) -> {
if (var5.getRollbackOnly()) {
var5.rollback();
} else {
var5.commit();
}

return var1x;
});
var9.onFailure((var2x) -> {
if (var5 != null) {
try {
if (var5.isActive()) {
var5.rollback();
}
} catch (Throwable var8) {
}
}

try {
var4.close();
} finally {
JPA.bindForAsync((EntityManager)null);
}

});
var9.onRedeem((var1x) -> {
try {
var4.close();
} finally {
JPA.bindForAsync((EntityManager)null);
}

});
return var9;
}
} catch (Throwable var16) {
if (var5 != null) {
try {
var5.rollback();
} catch (Throwable var15) {
}
}

if (var4 != null) {
try {
var4.close();
} finally {
JPA.bindForAsync((EntityManager)null);
}
}

throw var16;
}
}

public void shutdown() {
Iterator var1 = this.emfs.values().iterator();

while(var1.hasNext()) {
EntityManagerFactory var2 = (EntityManagerFactory)var1.next();
var2.close();
}

}

@Singleton
public static class JPAApiProvider implements Provider<JPAApi> {
private final JPAApi jpaApi;

@Inject
public JPAApiProvider(JPAConfig var1, DBApi var2, ApplicationLifecycle var3) {
this.jpaApi = new DefaultJPAApi(var1);
var3.addStopHook(() -> {
this.jpaApi.shutdown();
return Promise.pure((Object)null);
});
this.jpaApi.start();
}

public JPAApi get() {
return this.jpaApi;
}
}
}