package com.atm_search.cseh_17.atm_search.Model;

/**
 * Created by cseh_17 on 25.03.2018.
 */

public class Geometry
{
    private Viewport viewport;

    private Location location;

    public Viewport getViewport ()
    {
        return viewport;
    }

    public void setViewport (Viewport viewport)
    {
        this.viewport = viewport;
    }

    public Location getLocation ()
    {
        return location;
    }

    public void setLocation (Location location)
    {
        this.location = location;
    }

    @Override
    public String toString()
    {
        return "ClassPojo [viewport = "+viewport+", location = "+location+"]";
    }
}
