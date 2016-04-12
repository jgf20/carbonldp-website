package org.openrdf.sail.nativerdf;

import info.aduna.concurrent.locks.Lock;
import info.aduna.concurrent.locks.LockManager;
import info.aduna.io.MavenUtil;
import org.apache.commons.io.FileUtils;
import org.openrdf.IsolationLevel;
import org.openrdf.IsolationLevels;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.openrdf.query.algebra.evaluation.federation.FederatedServiceResolverImpl;
import org.openrdf.sail.NotifyingSailConnection;
import org.openrdf.sail.SailException;
import org.openrdf.sail.base.SailSource;
import org.openrdf.sail.base.SailStore;
import org.openrdf.sail.base.SnapshotSailStore;
import org.openrdf.sail.helpers.DirectoryLockManager;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author MiguelAraCo
 * @since 0.32.0
 */
public class SecuredNativeStore extends NativeStore {

	private static final String VERSION = MavenUtil.loadVersion( "org.openrdf.sesame", "sesame-sail-nativerdf", "devel" );

	/**
	 * Specifies which triple indexes this native store must use.
	 */
	private volatile String tripleIndexes;

	/**
	 * Flag indicating whether updates should be synced to disk forcefully. This
	 * may have a severe impact on write performance. By default, this feature is
	 * disabled.
	 */
	private volatile boolean forceSync = false;

	private volatile int valueCacheSize = ValueStore.VALUE_CACHE_SIZE;

	private volatile int valueIDCacheSize = ValueStore.VALUE_ID_CACHE_SIZE;

	private volatile int namespaceCacheSize = ValueStore.NAMESPACE_CACHE_SIZE;

	private volatile int namespaceIDCacheSize = ValueStore.NAMESPACE_ID_CACHE_SIZE;

	private SailStore store;

	/**
	 * Data directory lock.
	 */
	private volatile Lock dirLock;

	/**
	 * independent life cycle
	 */
	private FederatedServiceResolver serviceResolver;

	/**
	 * dependent life cycle
	 */
	private FederatedServiceResolverImpl dependentServiceResolver;

	/**
	 * Lock manager used to prevent concurrent {@link #getTransactionLock(IsolationLevel)} calls.
	 */
	private final ReentrantLock txnLockManager = new ReentrantLock();

	/**
	 * Holds locks for all isolated transactions.
	 */
	private final LockManager isolatedLockManager = new LockManager( debugEnabled() );

	/**
	 * Holds locks for all {@link IsolationLevels#NONE} isolation transactions.
	 */
	private final LockManager disabledIsolationLockManager = new LockManager( debugEnabled() );

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new NativeStore.
	 */
	public SecuredNativeStore() {
		super();
		setSupportedIsolationLevels( IsolationLevels.NONE, IsolationLevels.READ_COMMITTED,
			IsolationLevels.SNAPSHOT_READ, IsolationLevels.SNAPSHOT, IsolationLevels.SERIALIZABLE );
		setDefaultIsolationLevel( IsolationLevels.SNAPSHOT_READ );
	}

	public SecuredNativeStore( File dataDir ) {
		this();
		setDataDir( dataDir );
	}

	public SecuredNativeStore( File dataDir, String tripleIndexes ) {
		this( dataDir );
		setTripleIndexes( tripleIndexes );
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Sets the triple indexes for the native store, must be called before
	 * initialization.
	 *
	 * @param tripleIndexes
	 * 	An index strings, e.g. <tt>spoc,posc</tt>.
	 */
	public void setTripleIndexes( String tripleIndexes ) {
		if ( isInitialized() ) {
			throw new IllegalStateException( "sail has already been intialized" );
		}

		this.tripleIndexes = tripleIndexes;
	}

	public String getTripleIndexes() {
		return tripleIndexes;
	}

	/**
	 * Specifiec whether updates should be synced to disk forcefully, must be
	 * called before initialization. Enabling this feature may prevent corruption
	 * in case of events like power loss, but can have a severe impact on write
	 * performance. By default, this feature is disabled.
	 */
	public void setForceSync( boolean forceSync ) {
		this.forceSync = forceSync;
	}

	public boolean getForceSync() {
		return forceSync;
	}

	public void setValueCacheSize( int valueCacheSize ) {
		this.valueCacheSize = valueCacheSize;
	}

	public void setValueIDCacheSize( int valueIDCacheSize ) {
		this.valueIDCacheSize = valueIDCacheSize;
	}

	public void setNamespaceCacheSize( int namespaceCacheSize ) {
		this.namespaceCacheSize = namespaceCacheSize;
	}

	public void setNamespaceIDCacheSize( int namespaceIDCacheSize ) {
		this.namespaceIDCacheSize = namespaceIDCacheSize;
	}

	/**
	 * @return Returns the SERVICE resolver.
	 */
	public synchronized FederatedServiceResolver getFederatedServiceResolver() {
		if ( serviceResolver == null ) {
			if ( dependentServiceResolver == null ) {
				dependentServiceResolver = new FederatedServiceResolverImpl();
			}
			return serviceResolver = dependentServiceResolver;
		}
		return serviceResolver;
	}

	/**
	 * Overrides the {@link FederatedServiceResolver} used by this instance, but
	 * the given resolver is not shutDown when this instance is.
	 *
	 * @param resolver
	 * 	The SERVICE resolver to set.
	 */
	public synchronized void setFederatedServiceResolver( FederatedServiceResolver resolver ) {
		this.serviceResolver = resolver;
	}

	/**
	 * Initializes this NativeStore.
	 *
	 * @throws SailException
	 * 	If this NativeStore could not be initialized using the
	 * 	parameters that have been set.
	 */
	@Override
	protected void initializeInternal()
		throws SailException {
		logger.debug( "Initializing NativeStore..." );

		// Check initialization parameters
		File dataDir = getDataDir();

		if ( dataDir == null ) {
			throw new SailException( "Data dir has not been set" );
		} else if ( ! dataDir.exists() ) {
			boolean success = dataDir.mkdirs();
			if ( ! success ) {
				throw new SailException( "Unable to create data directory: " + dataDir );
			}
		} else if ( ! dataDir.isDirectory() ) {
			throw new SailException( "The specified path does not denote a directory: " + dataDir );
		} else if ( ! dataDir.canRead() ) {
			throw new SailException( "Not allowed to read from the specified directory: " + dataDir );
		}

		// try to lock the directory or fail
		dirLock = new DirectoryLockManager( dataDir ).lockOrFail();

		logger.debug( "Data dir is " + dataDir );

		try {
			File versionFile = new File( dataDir, "nativerdf.ver" );
			String version = versionFile.exists() ? FileUtils.readFileToString( versionFile ) : null;
			if ( ! VERSION.equals( version ) && upgradeStore( dataDir, version ) ) {
				FileUtils.writeStringToFile( versionFile, VERSION );
			}
			final NativeSailStore master = new SecuredNativeSailStore( dataDir, tripleIndexes, forceSync, valueCacheSize, valueIDCacheSize, namespaceCacheSize, namespaceIDCacheSize );
			this.store = new SnapshotSailStore( master, () -> new MemoryOverflowModel() {

				@Override
				protected SailStore createSailStore( File dataDir1 )
					throws IOException, SailException {
					// Model can't fit into memory, use another NativeSailStore to store delta
					return new SecuredNativeSailStore( dataDir1, getTripleIndexes() );
				}
			} ) {

				@Override
				public SailSource getExplicitSailSource() {
					if ( isIsolationDisabled() ) {
						// no isolation, use NativeSailStore directly
						return master.getExplicitSailSource();
					} else {
						return super.getExplicitSailSource();
					}
				}

				@Override
				public SailSource getInferredSailSource() {
					if ( isIsolationDisabled() ) {
						// no isolation, use NativeSailStore directly
						return master.getInferredSailSource();
					} else {
						return super.getInferredSailSource();
					}
				}
			};
		} catch ( Throwable e ) {
			// NativeStore initialization failed, release any allocated files
			dirLock.release();

			throw new SailException( e );
		}

		logger.debug( "NativeStore initialized" );
	}

	@Override
	protected void shutDownInternal()
		throws SailException {
		logger.debug( "Shutting down NativeStore..." );

		try {
			store.close();

			logger.debug( "NativeStore shut down" );
		} finally {
			dirLock.release();
			if ( dependentServiceResolver != null ) {
				dependentServiceResolver.shutDown();
			}
			logger.debug( "NativeStore shut down" );
		}
	}

	public boolean isWritable() {
		return getDataDir().canWrite();
	}

	@Override
	protected NotifyingSailConnection getConnectionInternal()
		throws SailException {
		try {
			return new NativeStoreConnection( this );
		} catch ( IOException e ) {
			throw new SailException( e );
		}
	}

	public ValueFactory getValueFactory() {
		return store.getValueFactory();
	}

	/**
	 * This call will block when {@link IsolationLevels#NONE} is provided when
	 * there are active transactions with a higher isolation and block when a
	 * higher isolation is provided when there are active transactions with
	 * {@link IsolationLevels#NONE} isolation. Store is either exclusively in
	 * {@link IsolationLevels#NONE} isolation with potentially zero or more
	 * transactions, or exclusively in higher isolation mode with potentially
	 * zero or more transactions.
	 *
	 * @param level
	 * 	indicating desired mode {@link IsolationLevels#NONE} or higher
	 * @return Lock used to prevent Store from switching isolation modes
	 * @throws SailException
	 */
	protected Lock getTransactionLock( IsolationLevel level )
		throws SailException {
		txnLockManager.lock();
		try {
			if ( IsolationLevels.NONE.isCompatibleWith( level ) ) {
				// make sure no isolated transaction are active
				isolatedLockManager.waitForActiveLocks();
				// mark isolation as disabled
				return disabledIsolationLockManager.createLock( level.toString() );
			} else {
				// make sure isolation is not disabled
				disabledIsolationLockManager.waitForActiveLocks();
				// mark isolated transaction as active
				return isolatedLockManager.createLock( level.toString() );
			}
		} catch ( InterruptedException e ) {
			Thread.currentThread().interrupt();
			throw new SailException( e );
		} finally {
			txnLockManager.unlock();
		}
	}

	/**
	 * Checks if any {@link IsolationLevels#NONE} isolation transactions are
	 * active.
	 *
	 * @return <code>true</code> if at least one transaction has direct access to
	 * the indexes
	 */
	boolean isIsolationDisabled() {
		return disabledIsolationLockManager.isActiveLock();
	}

	SailStore getSailStore() {
		return store;
	}

	private boolean upgradeStore( File dataDir, String version )
		throws IOException, SailException {
		if ( version == null ) {
			// either a new store or a pre-2.8.2 store
			ValueStore valueStore = new ValueStore( dataDir );
			try {
				valueStore.checkConsistency();
				return true; // good enough
			} catch ( SailException e ) {
				// valueStore is not consistent - possibly contains two entries for
				// string-literals with the same lexical value (e.g. "foo" and
				// "foo"^^xsd:string). Log an error and indicate upgrade should
				// not be executed.
				logger.error(
					"VALUE INCONSISTENCY: could not automatically upgrade native store to RDF 1.1-compatibility: {}. Failure to upgrade may result in inconsistent query results when comparing literal values.",
					e.getMessage() );
				return false;
			} finally {
				valueStore.close();
			}
		} else {
			return false; // no upgrade needed
		}
	}
}
