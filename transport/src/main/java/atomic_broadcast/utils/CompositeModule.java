package atomic_broadcast.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static atomic_broadcast.utils.ModuleName.Composite;

public class CompositeModule implements Module {

    @Override
    public ModuleName name() {
        return Composite;
    }

    private List<Module> modules = new ArrayList<>(20);

    public void add(Module module) {
        modules.add(module);
    }

    @Override
    public void start() {
        modules.forEach(Module::start);
    }

    @Override
    public void close() {
        Collections.reverse(modules);
        modules.forEach(Module::close);
    }

    public void poll() {
        for (int i = 0; i < modules.size(); i++) {
            modules.get(i).poll();
        }
    }

    public List<Module> getModules() {
        return modules;
    }
}
