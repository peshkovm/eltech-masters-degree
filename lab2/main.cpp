#include <iostream>
#include "omp.h"
#include <stdlib.h> // srand, rand
#include <time.h>   // time
#include <math.h>
#include <bits/stdc++.h>
#include <w32api/sysinfoapi.h>
#include <unistd.h>

using namespace std;

void swap(int &from, int &to) {
    int tmp = from;
    from = to;
    to = tmp;
}

void trickleDown(int arr[], int lastPos, int parent) {
    int left = 2 * parent + 1;
    int right = 2 * parent + 2;
    int largest = parent;

    if (left <= lastPos && arr[left] > arr[largest]) {
        largest = left;
    }

    if (right <= lastPos && arr[right] > arr[largest]) {
        largest = right;
    }

    if (largest != parent) {
        swap(arr[parent], arr[largest]);
        trickleDown(arr, lastPos, largest);
    }
}

void trickleUp(int arr[], int lastPos, int position) {
    if (position == 0)
        return;

    int parent = (int) floor((position - 1) / 2);

    if (arr[position] > arr[parent]) {
        swap(arr[position], arr[parent]);
        trickleUp(arr, lastPos, parent);
    }
}

void remove(int arr[], int &lastPos) {
    swap(arr[0], arr[lastPos--]);
    trickleDown(arr, lastPos, 0);
}

void add(int arr[], int newElement) {
    static int i = -1;
    arr[++i] = newElement;
    //trickleUp(arr, i, i);
}

/* A utility function to print array of size n */
void printArray(int arr[], int n) {
    for (int i = 0; i < n; ++i)
        cout << arr[i] << " ";
    cout << "\n";
}

int swapArrays(int *arr, int arrLength) {
    int pivotPoint = arrLength - 1;
    int value = arr[pivotPoint];
    int counter = 0;

    for (int i = 0; i < pivotPoint; i++) {
        if (arr[i] <= value) {
            swap(arr[i], arr[counter]);
            counter++;
        }
    }
    swap(arr[counter], arr[pivotPoint]);

    return counter;
}

void heapSortParallel(int arr[], const int arrLength, const int subArrLengthToDivide) {
    if (arrLength <= 1)
        return;

    int midLocation = swapArrays(arr, arrLength);

    int leftArrLength = midLocation - 0;
    int rightArrLength = arrLength - midLocation;
    int *leftArr = arr;
    int *rightArr = leftArr + leftArrLength;

#pragma omp parallel sections
    {
#pragma omp section
        {
#pragma omp critical
            {
                cout << "Left sub array: " << endl;
                cout << "   Thread # = " << omp_get_thread_num() << "   ";
                cout << "   Arr length = " << leftArrLength << endl;
            }

            if (leftArrLength > subArrLengthToDivide)
                heapSortParallel(leftArr, leftArrLength, subArrLengthToDivide);
            else if (leftArrLength > 1) {
                int lastPos = -1;

                for (int i = 0; i < leftArrLength; i++) {
                    lastPos++;
                    trickleUp(leftArr, lastPos, lastPos);
                }

                for (int i = 0; i < leftArrLength - 1; i++) {
                    remove(leftArr, lastPos);
                }
            }
        }

#pragma omp section
        {
#pragma omp critical
            {
                cout << "Right sub array: " << endl;
                cout << "   Thread # = " << omp_get_thread_num() << "   ";
                cout << "   Arr length = " << leftArrLength << endl;
            }

            if (rightArrLength > subArrLengthToDivide)
                heapSortParallel(rightArr, rightArrLength, subArrLengthToDivide);
            else if (rightArrLength > 1) {
                int lastPos = -1;

                for (int i = 0; i < rightArrLength; i++) {
                    lastPos++;
                    trickleUp(rightArr, lastPos, lastPos
                    );
                }

                for (int i = 0; i < rightArrLength - 1; i++) {
                    remove(rightArr, lastPos);
                }
            }
        }
    }
}

int main() {
    int arrLength = 0;

    cout << "Enter count of elements in array: ";
    cin >> arrLength;

    int *arr = new int[arrLength];

    cout << endl;

    for (int i = 0; i < arrLength; i++) {
        arr[i] = 0; //clear array for beautiful output
    }

    srand(time(nullptr));

    double start = omp_get_wtime();

    for (int i = 0; i < arrLength; i++) {
        int randomNumber = 1 + (rand() % (arrLength - 1 + 1));
        add(arr, randomNumber);
    }

    SYSTEM_INFO systemInfo;
    GetSystemInfo(&systemInfo);
    int numCPU = systemInfo.dwNumberOfProcessors;

    double stop = omp_get_wtime();

    double execTime = stop - start;

    cout << "In array: " << endl;
    printArray(arr, arrLength);
    cout << endl;
    cout << "Num of threads = " << numCPU << endl << endl;

    start = omp_get_wtime();

    //omp_set_num_threads(numCPU);

    heapSortParallel(arr, arrLength, arrLength / numCPU);

    stop = omp_get_wtime();

    cout << endl;
    cout << "Execution time  = " << execTime + (stop - start) << endl;
    cout << "Result:" << endl;
    printArray(arr, arrLength);
}
