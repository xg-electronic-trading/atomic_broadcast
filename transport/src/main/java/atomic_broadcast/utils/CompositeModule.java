package atomic_broadcast.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static atomic_broadcast.utils.ModuleName.Composite;

public class CompositeModule implements Module {

    private boolean started = false;

    @Override
    public ModuleName name() {
        return Composite;
    }

    @Override
    public InstanceInfo instanceInfo() {
        return null;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    private List<Module> modules = new ArrayList<>(20);

    public void add(Module module) {
        modules.add(module);
    }

    @Override
    public void start() {
        modules.forEach(m -> {
            if (!m.isStarted()) {
                m.start();
            }
        });
        started = true;
    }

    @Override
    public void close() {
        Collections.reverse(modules);
        modules.forEach(Module::close);
        started = false;
    }

    public List<Module> getModules() {
        return modules;
    }
}
