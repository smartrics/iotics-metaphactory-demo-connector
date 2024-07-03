package smartrics.iotics.samples.cars;

import java.util.List;
import java.util.Optional;

public record Binding(String pointID, String pointName, String valueID, String valueKey) {

    static Optional<Binding> find(List<Binding> bindings, String pointName, String valueKey) {
        return bindings.stream().filter(binding ->
                binding.pointName.equals(pointName) && binding.valueKey.equals(valueKey)).findFirst();
    }
}

