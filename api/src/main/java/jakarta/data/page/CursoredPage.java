/*
 * Copyright (c) 2022,2024 Contributors to the Eclipse Foundation
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
package jakarta.data.page;

import jakarta.data.repository.OrderBy;
import jakarta.data.Sort;
import java.util.NoSuchElementException;

/**
 * <p>A slice of data with the ability to create a cursor from the
 * keyset of each entity in the slice.</p>
 *
 * <p>Keyset cursor pagination is a form of pagination that aims to reduce the
 * possibility of missed or duplicate results by making the request for
 * each subsequent page relative to the observed values of entity properties
 * from the current page. This list of values is referred to as the keyset cursor
 * and is an ordered list of the values of entity properties that are in the
 * combined sort criteria of the repository method, in the same order of precedence.
 * The combination of sort criteria must uniquely identify each entity.
 * The keyset values can be from the last entity on the page
 * (for the next page in a forward direction) or first entity on the page
 * (if requesting the previous page),
 * or can be any other desired list of values which serve as a new starting
 * point. Keyset pagination also has the potential to improve performance
 * by avoiding the fetching and ordering of results from prior pages
 * because these become non-matching.</p>
 *
 * <p>To use keyset pagination, define a repository method with return value of
 * {@link CursoredPage} and which accepts a special parameter (after the normal
 * query parameters) that is a {@link PageRequest}. For example,</p>
 *
 * <pre>
 * &#64;OrderBy("lastName")
 * &#64;OrderBy("firstName")
 * &#64;OrderBy("id")
 * CursoredPage&lt;Employee&gt; findByHoursWorkedGreaterThan(int hours, {@code PageRequest<Employee>} pageRequest);
 * </pre>
 *
 * <p>You can use an offset-based {@link PageRequest} to request an initial page,</p>
 *
 * <pre>
 * page = employees.findByHoursWorkedGreaterThan(1500, PageRequest.of(Employee.class).size(50));
 * </pre>
 *
 * <p>For subsequent pages, you can request pagination relative to the
 * end of the current page as follows,</p>
 *
 * <pre>
 * page = employees.findByHoursWorkedGreaterThan(1500, page.nextPageRequest());
 * </pre>
 *
 * <p>Because the page is keyset aware, the {@link PageRequest}
 * that it returns from the call to {@link CursoredPage#nextPageRequest}
 * above is based upon a keyset cursor from that page to use as a starting point
 * after which the results for the next page are to be found.</p>
 *
 * <p>You can also construct a {@link PageRequest} with a {@link PageRequest.Cursor Cursor} directly, which
 * allows you to make it relative to a specific list of values. The number and
 * order of values must match that of the {@link OrderBy} annotations,
 * {@link Sort} parameters of the page request,
 * or <code>OrderBy</code> name pattern of the repository method.
 * For example,</p>
 *
 * <pre>
 * Employee emp = ...
 * {@code PageRequest<Employee>} pageRequest = PageRequest.of(Employee.class)
 *                                                .size(50)
 *                                                .afterKeyset(emp.lastName, emp.firstName, emp.id);
 * page = employees.findByHoursWorkedGreaterThan(1500, pageRequest);
 * </pre>
 *
 * <p>By making the query for the next page relative to observed values,
 * not a numerical position, keyset pagination is less vulnerable to changes
 * that are made to data in between page requests. Adding or removing entities
 * is possible without causing unexpected missed or duplicate results.
 * Keyset pagination does not prevent misses and duplicates if the entity
 * properties which are the sort criteria for existing entities are modified
 * or if an entity is re-added with different sort criteria after having
 * previously been removed.</p>
 *
 * <h2>Keyset Pagination with &#64;Query</h2>
 *
 * <p>Keyset pagination involves generating and appending to the query
 * additional query conditions for the keyset properties. In order for that to
 * be possible, a user-provided
 * <a href="https://eclipse-ee4j.github.io/jakartaee-tutorial/#full-query-language-syntax">JPQL</a>
 * query must end with a
 * <code>WHERE</code> clause to which additional conditions can be appended.
 * Enclose the entire conditional expression of the <code>WHERE</code> clause
 * in parenthesis.
 * Sort criteria must be specified independently from the user-provided query,
 * either with the {@link OrderBy} annotation or
 * {@link Sort} parameters to {@link PageRequest}.
 * For example,</p>
 *
 * <pre>
 * &#64;Query("SELECT o FROM Customer o WHERE (o.ordersPlaced &gt;= ?1 OR o.totalSpent &gt;= ?2)")
 * &#64;OrderBy("zipcode")
 * &#64;OrderBy("birthYear")
 * &#64;OrderBy("id")
 * CursoredPage&lt;Customer&gt; getTopBuyers(int minOrders, float minSpent,
 *                                         {@code PageRequest<Customer>} pageRequest);
 * </pre>
 *
 * <p>Queries that are used with keyset pagination must return entities
 * because keyset cursors are created from the entity attribute values that
 * form the keyset.</p>
 *
 * <h2>Page Numbers and Totals</h2>
 *
 * <p>Page numbers, total numbers of elements across all pages, and total
 * count of pages are not accurate when keyset pagination is used and should
 * not be relied upon.</p>
 *
 * <h2>Database Support for Keyset Pagination</h2>
 *
 * <p>A repository method with return type of <code>CursoredPage</code> must
 * raise {@link UnsupportedOperationException} if the database is incapable
 * of keyset pagination.
 * </p>
 *
 * @param <T> the type of elements in this slice 
 */
public interface CursoredPage<T> extends Page<T> {
    /**
     * Returns a {@link PageRequest.Cursor Cursor} for keyset values at the
     * specified position.
     *
     * @param index position (0 is first) of a result on the page.
     * @return cursor for keyset values at the specified position.
     */
    PageRequest.Cursor getKeysetCursor(int index);

    /**
     * Returns {@code true} when it is possible to navigate to a previous
     * page of results or if it is necessary to request a previous page in
     * order to determine whether there are more previous results.
     * @return {@code false} if the current page is empty or if it is known
     *         that there is not a previous page.
     */
    boolean hasPrevious();

    /**
     * <p>Creates a request for the next page in a forward direction from
     * the current page. This method computes a keyset cursor from the last
     * entity of the current page and includes the cursor in the pagination
     * information so that it can be used to obtain the next page in a
     * forward direction according to the sort criteria and relative to that
     * entity.</p>
     *
     * @return pagination information for requesting the next page.
     * @throws NoSuchElementException if the current page is empty or if
     *         it is known that there is no next page.
     *         To avoid this exception, check for a {@code true} result
     *         of {@link #hasNext()} before invoking this method.
     */
    PageRequest<T> nextPageRequest();

    /**
     * <p>Creates a request for the previous page in a reverse direction from
     * the current page. This method computes a keyset cursor from the first
     * entity of the current page and includes the cursor in the pagination
     * information so that it can be used to obtain the previous slice in a
     * reverse direction to the sort criteria and relative to that entity.
     * Within a single page, results are not reversed and remain ordered
     * according to the sort criteria.</p>
     *
     * <p>Page numbers are not accurate and should not be relied upon when
     * using keyset pagination. Jakarta Data providers should aim to at least
     * avoid returning negative or <code>0</code> as page numbers when
     * traversing pages in the reverse direction (this might otherwise occur
     * when matching entities are added prior to the first page and the
     * previous page is requested) by assigning a page number of <code>1</code>
     * to such pages. This means that there can be multiple consecutive pages
     * numbered <code>1</code> and that
     * <code>currentPage.previousPageRequest().next().page()</code>
     * cannot be relied upon to return a page number that is equal to the
     * current page number.</p>
     *
     * @return pagination information for requesting the previous page.
     * @throws NoSuchElementException if the current page is empty or if
     *         it is known that there is no previous page.
     *         To avoid this exception, check for a {@code true} result
     *         of {@link #hasPrevious()} before invoking this method.
     */
    PageRequest<T> previousPageRequest();
}