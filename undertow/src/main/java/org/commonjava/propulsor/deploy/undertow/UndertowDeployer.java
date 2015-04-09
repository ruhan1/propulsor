package org.commonjava.propulsor.deploy.undertow;

import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.ServletException;

import org.commonjava.propulsor.boot.BootOptions;
import org.commonjava.propulsor.boot.BootStatus;
import org.commonjava.propulsor.deploy.Deployer;
import org.commonjava.propulsor.deploy.undertow.util.DeploymentInfoUtils;

public class UndertowDeployer
    implements Deployer
{

    @Inject
    private Instance<UndertowDeploymentProvider> deployments;

    @Inject
    private Instance<UndertowDeploymentDefaultsProvider> defaultsDeployerInstance;

    private Set<UndertowDeploymentProvider> deploymentProviders;

    private UndertowDeploymentDefaultsProvider deploymentDefaultsProvider;

    private BootStatus status;

    private Undertow server;

    protected UndertowDeployer()
    {
    }

    public UndertowDeployer( final Set<UndertowDeploymentProvider> deploymentProviders,
                             final UndertowDeploymentDefaultsProvider deploymentDefaultsProvider )
    {
        this.deploymentProviders = deploymentProviders;
        this.deploymentDefaultsProvider = deploymentDefaultsProvider;
    }

    @PostConstruct
    public void cdiInit()
    {
        deploymentProviders = new HashSet<>();
        for ( final UndertowDeploymentProvider fac : deployments )
        {
            deploymentProviders.add( fac );
        }

        if ( !defaultsDeployerInstance.isUnsatisfied() )
        {
            deploymentDefaultsProvider = defaultsDeployerInstance.get();
        }
    }

    public DeploymentInfo getDeployment( final String contextRoot, final String deploymentName )
    {
        final DeploymentInfo di = new DeploymentInfo().setContextPath( contextRoot )
                                                      .setDeploymentName( deploymentName )
                                                      .setClassLoader( ClassLoader.getSystemClassLoader() );

        if ( deploymentDefaultsProvider != null )
        {
            deploymentDefaultsProvider.setDefaults( di );
        }

        if ( deploymentProviders != null )
        {
            DeploymentInfoUtils.mergeFromProviders( di, deploymentProviders );
        }

        return di;
    }

    @Override
    public BootStatus deploy( final BootOptions options )
    {
        UndertowBootOptions bootOptions;
        if ( options instanceof UndertowBootOptions )
        {
            bootOptions = (UndertowBootOptions) options;
        }
        else
        {
            bootOptions = UndertowBootOptions.DEFAULT;
        }

        final DeploymentInfo di = getDeployment( bootOptions.getContextPath(), bootOptions.getDeploymentName() );

        final DeploymentManager dm = Servlets.defaultContainer()
                                             .addDeployment( di );
        dm.deploy();

        status = new BootStatus();
        try
        {
            server = Undertow.builder()
                             .setHandler( dm.start() )
                             .addHttpListener( bootOptions.getPort(), bootOptions.getBind() )
                             .build();

            server.start();
            status.markSuccess();

            System.out.printf( "AProx listening on %s:%s\n\n", bootOptions.getBind(), bootOptions.getPort() );

        }
        catch ( ServletException | RuntimeException e )
        {
            status.markFailed( BootStatus.ERR_CANT_LISTEN, e );
        }

        return status;
    }

    @Override
    public void stop()
    {
        if ( server != null && status.isSuccess() )
        {
            server.stop();
        }
    }

}
