/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 */
package jakarta.data;


import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.util.List;


class CompositeRestrictionRecordTest {
    // A mock entity class for tests
    static class Person {
    }

    @Test
    void shouldCreateCompositeRestrictionWithDefaultNegation() {
        Restriction<String> restriction1 = new BasicRestrictionRecord<>("title", Operator.EQUAL, "Java Guide");
        Restriction<String> restriction2 = new BasicRestrictionRecord<>("author", Operator.EQUAL, "John Doe");

        CompositeRestrictionRecord<String> composite = new CompositeRestrictionRecord<>(
                CompositeRestriction.Type.ALL,
                List.of(restriction1, restriction2)
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(composite.type()).isEqualTo(CompositeRestriction.Type.ALL);
            soft.assertThat(composite.restrictions()).containsExactly(restriction1, restriction2);
            soft.assertThat(composite.isNegated()).isFalse();
        });
    }

    @Test
    void shouldCreateCompositeRestrictionWithExplicitNegation() {
        Restriction<String> restriction1 = new BasicRestrictionRecord<>("title", Operator.EQUAL, "Java Guide");
        Restriction<String> restriction2 = new BasicRestrictionRecord<>("author", Operator.EQUAL, "John Doe");

        CompositeRestrictionRecord<String> composite = new CompositeRestrictionRecord<>(
                CompositeRestriction.Type.ANY,
                List.of(restriction1, restriction2),
                true
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(composite.type()).isEqualTo(CompositeRestriction.Type.ANY);
            soft.assertThat(composite.restrictions()).containsExactly(restriction1, restriction2);
            soft.assertThat(composite.isNegated()).isTrue();
        });
    }

    @Test
    void shouldFailIfEmptyRestrictions() {
        assertThatThrownBy(() -> new CompositeRestrictionRecord<>(CompositeRestriction.Type.ALL, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot create a composite restriction without any restrictions to combine.");
    }

    @Test
    void shouldNegateCompositeRestriction() {
        Restriction<Person> ageLessThan50 = Restrict.lessThan(50, "age");
        Restriction<Person> nameStartsWithDuke = Restrict.startsWith("Duke ", "name");
        Restriction<Person> all = Restrict.all(ageLessThan50, nameStartsWithDuke);
        Restriction<Person> allNegated = all.negate();
        Restriction<Person> notAll = Restrict.not(all);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(all.isNegated()).isEqualTo(false);
            soft.assertThat(((CompositeRestriction<Person>) all).restrictions()
                    .get(0).isNegated()).isEqualTo(false);
            soft.assertThat(((CompositeRestriction<Person>) all).restrictions()
                    .get(1).isNegated()).isEqualTo(false);

            soft.assertThat(allNegated.isNegated()).isEqualTo(true);
            soft.assertThat(((CompositeRestriction<Person>) allNegated).restrictions()
                    .get(0).isNegated()).isEqualTo(false);
            soft.assertThat(((CompositeRestriction<Person>) allNegated).restrictions()
                    .get(1).isNegated()).isEqualTo(false);

            soft.assertThat(notAll.isNegated()).isEqualTo(true);
            soft.assertThat(((CompositeRestriction<Person>) notAll).restrictions()
                    .get(0).isNegated()).isEqualTo(false);
            soft.assertThat(((CompositeRestriction<Person>) notAll).restrictions()
                    .get(1).isNegated()).isEqualTo(false);
        });
    }

    @Test
    void shouldPreserveRestrictionsOrder() {
        Restriction<String> restriction1 = new BasicRestrictionRecord<>("title", Operator.EQUAL, "Java Guide");
        Restriction<String> restriction2 = new BasicRestrictionRecord<>("author", Operator.EQUAL, "John Doe");

        CompositeRestrictionRecord<String> composite = new CompositeRestrictionRecord<>(
                CompositeRestriction.Type.ALL,
                List.of(restriction1, restriction2)
        );

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(composite.restrictions().get(0)).isEqualTo(restriction1);
            soft.assertThat(composite.restrictions().get(1)).isEqualTo(restriction2);
        });
    }

    @Test
    void shouldSupportNegationUsingDefaultConstructor() {
        // Given multiple restrictions
        Restriction<String> restriction1 = new BasicRestrictionRecord<>("title", Operator.EQUAL, "Java Guide");
        Restriction<String> restriction2 = new BasicRestrictionRecord<>("author", Operator.EQUAL, "John Doe");

        // When creating a composite restriction and manually setting negation
        CompositeRestrictionRecord<String> composite = new CompositeRestrictionRecord<>(
                CompositeRestriction.Type.ALL,
                List.of(restriction1, restriction2)
        );
        CompositeRestrictionRecord<String> negatedComposite = new CompositeRestrictionRecord<>(
                composite.type(),
                composite.restrictions(),
                true
        );

        // Then validate the negated composite restriction
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(negatedComposite.type()).isEqualTo(CompositeRestriction.Type.ALL);
            soft.assertThat(negatedComposite.restrictions()).containsExactly(restriction1, restriction2);
            soft.assertThat(negatedComposite.isNegated()).isTrue();
        });
    }
}
