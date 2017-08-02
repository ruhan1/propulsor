package org.commonjava.propulsor.deploy.camel.route;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.BuilderSupport;
import org.apache.camel.model.RouteDefinition;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * Provides routes during Camel application boot. To use, extend this class and implement the configure() method by calling
 * route() and using the fluent api it provides to setup a new route. Each time route() is called, a new route configuration
 * is created. Finally, after configure() exits, addRoutesToCamelContext() is called to add the configured routes to
 * the context.
 *
 * Subclasses should normally be initialized using CDI, which will inject a {@link RouteAliasManager} and call the
 * @PostConstruct {@link RoutingSetup#start()} method. This allows them to be used from {@link javax.enterprise.inject.Instance}
 * injections.
 */
public abstract class RoutingSetup
        extends BuilderSupport
        implements RoutesBuilder
{
    private AliasedRoutesDefinition routes;

    @Inject
    private RouteAliasManager aliasManager;

    protected RoutingSetup(){}

    protected RoutingSetup( RouteAliasManager aliasManager )
    {
        this.aliasManager = aliasManager;
        this.routes = new AliasedRoutesDefinition( aliasManager );
    }

    @PostConstruct
    public void start()
    {
        this.routes = new AliasedRoutesDefinition( aliasManager );
    }

    @Override
    public final void addRoutesToCamelContext( final CamelContext context )
            throws Exception
    {
        for ( RouteDefinition route : routes.getRoutes() )
        {
            context.addRouteDefinition( route );
        }
    }

    protected abstract void configure();

    protected final RouteDefinition route()
    {
        return routes.route();
    }
}
