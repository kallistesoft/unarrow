package ca.kallistesoft.unarrow.core.update

import javax.inject.Qualifier

/** The small preferences store holding the cached update rules. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ForceUpdateStore
