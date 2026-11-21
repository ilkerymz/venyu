package com.venyu.identity;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Diğer modüllere açık, salt okunur kullanıcı iletişim bilgisi (örn. organizatörün katılımcı listesi).
 * Toplu okuma: N kullanıcı için tek sorgu. Bulunamayan id'ler map'te yer almaz (silinmiş kullanıcı).
 */
public interface UserDirectory {

    Map<UUID, UserContact> findContacts(Collection<UUID> userIds);
}
