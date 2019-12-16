#include <omp.h>
#include <cmath>
#include <stdio.h>
#include <time.h>
#include <iostream>
#include <algorithm>
#include <w32api/sysinfoapi.h>

using namespace std;

#define ITERATION_LIMIT 555
#define EPSILON 0.001

bool check_diagoanally_dominant(float **matrix, int matrix_size);

void solve_seidel_sequential(float **matrix, int matrix_size, const float *right_hand_side);

void solve_seidel_parallel(float **matrix, int matrix_size, const float *right_hand_side);

void zeroed_array(float *array, int array_size);

float *clone_array(const float *array, int array_length);

void delete_matrix(float **matrix, int matrix_size);

int main() {
    int matrix_size;

    matrix_size = 0;
    cout << "Введите размер матрицы: ";
    cin >> matrix_size;
    cout << endl;

    float **matrix = new float *[matrix_size];
    for (int i = 0; i < matrix_size; i++)
        matrix[i] = new float[matrix_size];
    float *right_hand_side = new float[matrix_size];

    cout << "Введите элементы матрицы:" << endl;
    for (int i = 0; i < matrix_size; i++) {
        for (int j = 0; j < matrix_size; j++) {
            cout << "Matrix[" << i << "][" << j << "]: ";
            cin >> matrix[i][j];
        }
    }

    if (!check_diagoanally_dominant(matrix, matrix_size)) {
        cout << "Матрица не сходится" << endl;
        delete_matrix(matrix, matrix_size);
        delete[] right_hand_side;
        return 0;
    }

    cout << endl;
    cout << "Введите правые части уравнения:" << endl;

    for (int i = 0; i < matrix_size; i++) {
        cout << "Элемент №" << i << ": ";
        cin >> right_hand_side[i];
    }

    cout << endl;
    cout << "Окончательная система уравнений:" << endl;
    for (int i = 0; i < matrix_size; i++) {
        for (int j = 0; j < matrix_size; j++) {
            cout << matrix[i][j] << "x" << j << " ";
        }
        cout << "= " << right_hand_side[i] << endl;
    }

    cout << endl;
    cout << "Последовательный алгоритм -> 0,  Параллельный алгоритм -> 1" << endl;
    cout << "Ваш выбор: ";
    int run_mode_choice;
    cin >> run_mode_choice;
    cout << endl;

    switch (run_mode_choice) {
        //Последовательный алгоритм
        case 0: {
            double start = omp_get_wtime();

            solve_seidel_sequential(matrix, matrix_size, right_hand_side);

            double stop = omp_get_wtime();
            double execTime = stop - start;

            cout << endl;
            cout << "Время выполнения: " << execTime << endl;
            break;
        }
            //Параллельный алгоритм
        case 1: {
            SYSTEM_INFO systemInfo;
            GetSystemInfo(&systemInfo);
            int numCPU = systemInfo.dwNumberOfProcessors;
            omp_set_num_threads(numCPU);

            cout << "Количество потоков: " << numCPU << endl << endl;

            double start = omp_get_wtime();

            solve_seidel_parallel(matrix, matrix_size, right_hand_side);

            double stop = omp_get_wtime();
            double execTime = stop - start;

            cout << endl;
            cout << "Время выполнения: " << execTime << endl;
            break;
        }
    }

    delete_matrix(matrix, matrix_size);
    delete[] right_hand_side;
}

bool check_diagoanally_dominant(float **matrix, int matrix_size) {
    int isDominant = true;

    for (int i = 0; i < matrix_size && isDominant; i++) {
        float row_sum = 0;
        for (int j = 0; j < matrix_size; j++) {
            if (j != i) row_sum += abs(matrix[i][j]);
        }

        if (abs(matrix[i][i]) < row_sum) {
            isDominant = false;
        }
    }
    return isDominant;
}

void solve_seidel_sequential(float **matrix, int matrix_size, const float *right_hand_side) {
    float *solution = new float[matrix_size];
    float *last_iteration;
    float *d = new float[matrix_size];
    float maxD = 0;

    zeroed_array(solution, matrix_size);
    zeroed_array(d, matrix_size);

    cout << "        ";
    for (int j = 0; j < matrix_size; j++) {
        cout << "        x" << j;
    }
    cout << "        maxD" << endl;

    for (int i = 0; i < ITERATION_LIMIT; i++) {
        last_iteration = clone_array(solution, matrix_size);

        for (int j = 0; j < matrix_size; j++) {
            float x = 0;

            for (int k = 0; k < matrix_size; k++) {
                if (j != k) {
                    x += matrix[j][k] * solution[k];
                }
            }
            solution[j] = (right_hand_side[j] - x) / matrix[j][j];
        }

        for (int j = 0; j < matrix_size; j++) {
            d[j] = (abs(last_iteration[j] - solution[j]));
        }

        maxD = *max_element(d, d + matrix_size);

        cout << "Итерация №" << i << ": ";
        for (int l = 0; l < matrix_size; l++) {
            cout << solution[l] << " ";
        }
        cout << " " << maxD;
        cout << endl;

        if (maxD <= EPSILON) break;
    }

    cout << endl;
    cout << "Ответ:" << endl;
    for (int i = 0; i < matrix_size; i++) {
        cout << "x" << i << "= " << solution[i] << " +- " << EPSILON << endl;
    }
}

void solve_seidel_parallel(float **matrix, int matrix_size, const float *right_hand_side) {
    float *solution = new float[matrix_size];
    float *last_iteration;
    float *d = new float[matrix_size];
    float maxD = 0;

    zeroed_array(solution, matrix_size);
    zeroed_array(d, matrix_size);

    cout << "        ";
    for (int j = 0; j < matrix_size; j++) {
        cout << "        x" << j;
    }
    cout << "        maxD" << endl;

    for (int i = 0; i < ITERATION_LIMIT; i++) {
        last_iteration = clone_array(solution, matrix_size);

        // Each thread is assigned to a row to compute the corresponding solution element
#pragma omp parallel for schedule(dynamic, 1)
        for (int j = 0; j < matrix_size; j++) {
            float x = 0;

            for (int k = 0; k < matrix_size; k++) {
                if (j != k) {
                    x += matrix[j][k] * solution[k];
                }
            }
            solution[j] = (right_hand_side[j] - x) / matrix[j][j];
#pragma omp critical
            {
                cout << "Поток №: " << omp_get_thread_num() << "   ";
                for (int k = matrix_size - j; k < matrix_size; k++)
                    cout << "         ";

                cout << solution[j] << endl;
            }
        }

#pragma omp parallel for schedule(dynamic, 1)
        for (int j = 0; j < matrix_size; j++) {
            d[j] = (abs(last_iteration[j] - solution[j]));
        }

        maxD = *max_element(d, d + matrix_size);

        cout << "Итерация №" << i << ": ";
        for (int l = 0; l < matrix_size; l++) {
            cout << solution[l] << " ";
        }
        cout << " " << maxD;
        cout << endl;

        if (maxD <= EPSILON) break;
    }

    cout <<
         endl;
    cout << "Ответ:" <<
         endl;
    for (
            int i = 0;
            i < matrix_size;
            i++) {
        cout << "x" << i << "= " << solution[i] << " +- " << EPSILON <<
             endl;
    }
}

void zeroed_array(float *array, int array_size) {
    for (int i = 0; i < array_size; i++) {
        array[i] = 0;
    }
}

float *clone_array(const float *array, int array_length) {
    float *output = new float[array_length];
    for (int i = 0; i < array_length; i++) {
        output[i] = array[i];
    }
    return output;
}

void delete_matrix(float **matrix, int matrix_size) {
    for (int i = 0; i < matrix_size; i++) {
        delete[] matrix[i];
    }
    delete[] matrix;
}